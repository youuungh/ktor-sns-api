package com.ninezero.services

import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoDatabase
import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.*
import com.ninezero.models.error.ErrorCode
import com.ninezero.models.mappers.UserMapper
import com.ninezero.models.tables.*
import com.ninezero.services.social.SocialLoginProviderManager
import com.ninezero.utils.FileConfig
import com.ninezero.utils.Validation
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.reactive.awaitFirst
import org.bson.Document
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class UserServiceImpl(
    private val fileService: FileService,
    private val database: MongoDatabase,
    private val notificationService: NotificationService,
    private val socialLoginProviderManager: SocialLoginProviderManager
) : UserService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // DB 트랜잭션 처리
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(IO) { block() }

    // 회원 가입
    override suspend fun create(request: UserCreateRequest): Long = dbQuery {
        // 아이디 중복 검사
        Users.select(Users.loginId)
            .where { Users.loginId eq request.loginId }
            .firstOrNull()?.let {
                throw ValidationException(ErrorCode.USER_LOGIN_ID_ALREADY_EXISTED)
            }

        // 유효성 검사
        validateUserCreate(request)

        // 사용자 생성
        Users.insert {
            it[loginId] = request.loginId
            it[userName] = request.userName
            it[password] = BCrypt.hashpw(request.password, BCrypt.gensalt())
            it[extraUserInfo] = request.extraUserInfo
            it[profileImagePath] = request.profileImagePath
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }[Users.id].value
    }

    // 로그인 인증
    override suspend fun authenticate(request: LoginRequest): UserResponse = dbQuery {
//        val user = Users.selectAll()
//            .where { Users.loginId eq request.loginId }
//            .firstOrNull()?.let { row ->
//                val password = row[Users.password]
//                if (BCrypt.checkpw(request.password, password)) {
//                    UserMapper.toUserResponse(row)
//                } else null
//            } ?: throw ValidationException(CustomError.USER_FAIL_LOGIN)
//
//        user

        val user = Users.select(Users.columns)
            .where { Users.loginId eq request.loginId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.USER_FAIL_LOGIN, "존재하지 않는 아이디입니다")

        val password = user[Users.password]
        if (!BCrypt.checkpw(request.password, password)) {
            throw ValidationException(ErrorCode.USER_FAIL_LOGIN, "비밀번호가 일치하지 않습니다")
        }

        UserMapper.toUserResponse(user)
    }

    override suspend fun authenticateSocial(request: SocialLoginRequest): UserResponse = dbQuery {
        // 소셜 로그인 제공자 가져오기
        val provider = socialLoginProviderManager.getProvider(request.provider)

        // 토큰 검증 및 사용자 정보 가져오기
        val socialUserInfo = provider.verifyToken(request.token)

        // 소셜 ID로 기존 사용자 찾기
        val socialId = "social:${socialUserInfo.provider}:${socialUserInfo.id}"
        val existingUser = Users.selectAll().where { Users.extraUserInfo eq socialId }.firstOrNull()

        if (existingUser != null) {
            val userId = existingUser[Users.id].value
            val boardCount = getBoardCounts(userId)
            val (followerCount, followingCount, isFollowing) = getUserFollowCounts(userId, userId)
            return@dbQuery UserMapper.toUserResponse(
                existingUser,
                boardCount,
                followerCount,
                followingCount,
                isFollowing
            )
        } else {
            val baseLoginId = socialUserInfo.emailId ?: socialUserInfo.userName
            var candidateId = baseLoginId
            var counter = 1

            while (Users.selectAll().where { Users.loginId eq candidateId }.count() > 0) {
                candidateId = "$baseLoginId$counter"
                counter++
            }

            val insertedUser = Users.insert {
                it[this.loginId] = candidateId
                it[userName] = socialUserInfo.userName
                it[password] = ""
                it[extraUserInfo] = socialId
                it[profileImagePath] = socialUserInfo.profileImage
                it[createdAt] = LocalDateTime.now()
                it[updatedAt] = LocalDateTime.now()
            }

            val userId = insertedUser[Users.id].value
            val (followerCount, followingCount, isFollowing) = getUserFollowCounts(userId, userId)

            val user = Users.selectAll().where { Users.id eq userId }.first()
            return@dbQuery UserMapper.toUserResponse(
                user,
                0,
                followerCount,
                followingCount,
                isFollowing
            )
        }
    }

    // 전체 사용자 목록 조회
    override suspend fun getAllUsers(page: Int, size: Int, requesterId: Long): List<UserResponse> = dbQuery {
        val users = Users.select(Users.columns)
            .where { Users.id neq requesterId } // 현재 사용자 제외
            .orderBy(Users.id to SortOrder.DESC)
            .limit(size)
            .offset(start = ((page - 1) * size).toLong())
            .toList()

        if (users.isEmpty()) return@dbQuery emptyList()

        users.map { userRow ->
            val userId = userRow[Users.id].value
            val boardCount = getBoardCounts(userId)
            val (followerCount, followingCount, isFollowing) = getUserFollowCounts(userId, requesterId)
            UserMapper.toUserResponse(userRow, boardCount, followerCount, followingCount, isFollowing)
        }
    }

    // 사용자 정보 조회
    override suspend fun getById(id: Long, requesterId: Long): UserResponse = dbQuery {
        val user = Users.select(Users.columns)
            .where { Users.id eq id }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)

        val boardCount = getBoardCounts(id)
        val (followerCount, followingCount, isFollowing) = getUserFollowCounts(id, requesterId)
        UserMapper.toUserResponse(user, boardCount, followerCount, followingCount, isFollowing)
    }

    // 사용자 정보 수정
    override suspend fun update(id: Long, request: UserUpdateRequest): UserResponse = dbQuery {
        // 유효성 검사
        validateUserUpdate(request)

        // 기존 사용자 정보 조회
        val userInfo = Users.select(Users.userName, Users.profileImagePath)
            .where { Users.id eq id }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)

        val oldUserName = userInfo[Users.userName]
        val existingProfileImagePath = userInfo[Users.profileImagePath]
        val newUserName = request.userName
        val newProfileImagePath = request.profileImagePath

        // 프로필 이미지가 변경된 경우
        request.profileImagePath?.let { newProfileImage ->
            // 기존 프로필 이미지 삭제
            existingProfileImagePath?.let { oldProfileImageId ->
                try {
                    FileConfig.extractFileId(oldProfileImageId)?.let {
                        fileService.deleteFile(it)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to delete profile image: ID=$oldProfileImageId", e)
                }
            }

            // 작성자로 작성한 게시물 업데이트
            Boards.update({ Boards.createUserId eq id }) {
                it[createUserProfileImagePath] = newProfileImage
            }

            // 수정자로 수정한 게시물 업데이트
            Boards.update({ Boards.updateUserId eq id }) {
                it [updateUserProfileImagePath] = newProfileImage
            }

            // 댓글의 프로필 이미지 업데이트
            Comments.update({ Comments.createUserId eq id }) {
                it[profileImageUrl] = newProfileImage
            }
        }

        // 기본 사용자 정보 업데이트
        Users.update({ Users.id eq id }) {
            it[userName] = request.userName
            it[extraUserInfo] = request.extraUserInfo
            it[profileImagePath] = request.profileImagePath
            it[updatedAt] = LocalDateTime.now()
        }

        // MongoDB 채팅방 참가자 정보 업데이트
        updateChatRoomParticipantInfo(id, newUserName, newProfileImagePath, oldUserName)

        getById(id, id)
    }

    // 채팅방 참가자 정보 업데이트
    private suspend fun updateChatRoomParticipantInfo(
        userId: Long,
        newUserName: String,
        newProfileImagePath: String?,
        oldUserName: String
    ) {
        try {
            val roomsCollection = database.getCollection("chat_rooms")
            val updateFields = Document()

            // 사용자 이름은 항상 업데이트 (변경 여부와 관계없이)
            updateFields.append("participants.$.userName", newUserName)

            // 프로필 이미지가 제공된 경우에만 업데이트
            if (newProfileImagePath != null) {
                updateFields.append("participants.$.profileImagePath", newProfileImagePath)
            }

            // 참가자 정보 업데이트
            roomsCollection.updateMany(
                Filters.elemMatch("participants", Filters.eq("userId", userId)),
                Document("\$set", updateFields)
            ).awaitFirst()

            // 사용자 이름이 변경된 경우 메시지의 사용자 이름도 업데이트
            if (newUserName != oldUserName) {
                val messagesCollection = database.getCollection("messages")
                messagesCollection.updateMany(
                    Filters.eq("senderId", userId),
                    Document("\$set", Document("senderName", newUserName))
                ).awaitFirst()
            }

            logger.info("Updated user info in chat database for userId: $userId")
        } catch (e: Exception) {
            logger.error("Failed to update user info in chat database: ${e.message}", e)
        }
    }

    // 팔로우
    override suspend fun follow(followerId: Long, followingId: Long): Long = dbQuery {
        if (followerId == followingId) {
            throw ValidationException(ErrorCode.INVALID_FOLLOW_REQUEST)
        }

        // 팔로우 대상 사용자 조회
        Users.select(Users.id)
            .where { Users.id eq followingId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)

        val exists = Follows
            .select(Follows.id)
            .where {
                Follows.followerId eq followerId and
                        (Follows.followingId eq followingId)
            }
            .firstOrNull()

        if (exists != null) {
            throw ValidationException(ErrorCode.ALREADY_FOLLOWING)
        }

        // 팔로우하는 사용자 정보 조회
        val follower = Users.select(Users.userName, Users.loginId, Users.profileImagePath)
            .where { Users.id eq followerId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)

        val followId = Follows.insert {
            it[this.followerId] = followerId
            it[this.followingId] = followingId
            it[createdAt] = LocalDateTime.now()
        }[Follows.id].value

        val notification = FCMNotification(
            type = "follow",
            title = "새 팔로워",
            body = "${follower[Users.userName]}님이 회원님을 팔로우하기 시작했습니다.",
            userId = followerId
        )

        notificationService.sendNotificationToUser(followingId, notification)

        followId
    }

    // 팔로우 취소
    override suspend fun unfollow(followerId: Long, followingId: Long): Long = dbQuery {
        Users.select(Users.id)
            .where { Users.id eq followingId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)

        val deleted = Follows.deleteWhere {
            Follows.followerId eq followerId and
                    (Follows.followingId eq followingId)
        }
        if (deleted == 0) throw ValidationException(ErrorCode.NOT_FOLLOWING)
        followingId
    }

    private fun validateUserCreate(request: UserCreateRequest) {
        if (!Validation.validateLoginId(request.loginId)) {
            throw ValidationException(ErrorCode.INVALID_PARAMETER)
        }
        if (!Validation.validatePassword(request.password)) {
            throw ValidationException(ErrorCode.INVALID_PARAMETER)
        }
        if (!Validation.validateName(request.userName)) {
            throw ValidationException(ErrorCode.INVALID_PARAMETER)
        }
    }

    private fun validateUserUpdate(request: UserUpdateRequest) {
        if (!Validation.validateName(request.userName)) {
            throw ValidationException(ErrorCode.INVALID_PARAMETER)
        }
    }

    private suspend fun getBoardCounts(userId: Long): Int = dbQuery {
        Boards.select(Boards.id)
            .where { Boards.createUserId eq userId }
            .count()
            .toInt()
    }

    private suspend fun getUserFollowCounts(userId: Long, requesterId: Long): Triple<Int, Int, Boolean> = dbQuery {
        val followerCount = Follows
            .select(Follows.followingId)
            .where { Follows.followingId eq userId }
            .count()

        val followingCount = Follows
            .select(Follows.followerId)
            .where { Follows.followerId eq userId }
            .count()

        val isFollowing = Follows
            .select(Follows.id)
            .where {
                Follows.followerId eq requesterId and
                        (Follows.followingId eq userId)
            }
            .firstOrNull() != null

        Triple(followerCount.toInt(), followingCount.toInt(), isFollowing)
    }

    override suspend fun searchUsers(
        query: String,
        page: Int,
        size: Int,
        requesterId: Long
    ): List<UserResponse> = dbQuery {
        val searchQuery = "%${query.lowercase()}%"

        val users = Users.select(Users.columns)
            .where {
                (Users.userName.lowerCase() like searchQuery) or
                        (Users.loginId.lowerCase() like searchQuery) and
                        (Users.id neq requesterId) // 자기 자신 제외
            }
            .orderBy(Users.userName to SortOrder.ASC)
            .limit(size)
            .offset(start = ((page - 1) * size).toLong())
            .toList()

        if (users.isEmpty()) return@dbQuery emptyList()

        users.map { userRow ->
            val userId = userRow[Users.id].value
            val boardCount = getBoardCounts(userId)
            val (followerCount, followingCount, isFollowing) = getUserFollowCounts(userId, requesterId)
            UserMapper.toUserResponse(
                row = userRow,
                boardCount = boardCount,
                followerCount = followerCount,
                followingCount = followingCount,
                isFollowing = isFollowing
            )
        }
    }

    override suspend fun saveRecentSearch(userId: Long, searchedUserId: Long) = dbQuery {
        // 자기 자신은 최근 검색에 저장하지 않음
        if (userId == searchedUserId) return@dbQuery

        // 이미 존재하는 검색 기록 삭제
        RecentSearches.deleteWhere {
            (RecentSearches.userId eq userId) and
                    (RecentSearches.searchedUserId eq searchedUserId)
        }

        // 검색된 사용자 정보 가져오기
        val searchedUser = Users.select(Users.columns)
            .where { Users.id eq searchedUserId }
            .firstOrNull() ?: return@dbQuery

        // 새로운 검색 기록 추가
        RecentSearches.insert {
            it[this.userId] = userId
            it[this.searchedUserId] = searchedUserId
            it[searchedUserName] = searchedUser[Users.userName]
            it[searchedUserProfileImagePath] = searchedUser[Users.profileImagePath]
            it[searchedAt] = LocalDateTime.now()
        }
    }

    override suspend fun getRecentSearches(userId: Long, limit: Int): List<RecentSearch> = dbQuery {
        RecentSearches
            .select(RecentSearches.columns)
            .where { RecentSearches.userId eq userId }
            .orderBy(RecentSearches.searchedAt to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                RecentSearch(
                    id = row[RecentSearches.id].value,
                    userId = row[RecentSearches.userId],
                    searchedUserId = row[RecentSearches.searchedUserId],
                    searchedUserName = row[RecentSearches.searchedUserName],
                    searchedUserProfileImagePath = row[RecentSearches.searchedUserProfileImagePath],
                    searchedAt = row[RecentSearches.searchedAt]
                )
            }
    }

    override suspend fun deleteRecentSearch(userId: Long, searchedUserId: Long) = dbQuery {
        RecentSearches.deleteWhere {
            (RecentSearches.userId eq userId) and
                    (RecentSearches.searchedUserId eq searchedUserId)
        }
        Unit
    }

    override suspend fun clearRecentSearches(userId: Long) = dbQuery {
        RecentSearches.deleteWhere { RecentSearches.userId eq userId }
        Unit
    }
}

/**
class UserServiceImpl(
    private val config: ApplicationConfig
) : UserService {
    private val uploadPath = File(config.property("file.uploadPath").getString()).absolutePath
    private val baseUrl = config.property("file.baseUrl").getString()

    // DB 트랜잭션 처리
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(IO) { block() }

    // 회원 가입
    override suspend fun create(request: UserCreateRequest): Long = dbQuery {
        // 아이디 중복 검사
        Users.select(Users.loginId)
            .where { Users.loginId eq request.loginId }
            .firstOrNull()?.let {
                throw ValidationException(ErrorCode.USER_LOGIN_ID_ALREADY_EXISTED)
            }

        // 유효성 검사
        validateUserCreate(request)

        // 사용자 생성
        Users.insert {
            it[loginId] = request.loginId
            it[userName] = request.userName
            it[password] = BCrypt.hashpw(request.password, BCrypt.gensalt())
            it[extraUserInfo] = request.extraUserInfo
            it[profileImagePath] = request.profileImagePath
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }[Users.id].value
    }

    // 로그인 인증
    override suspend fun authenticate(request: LoginRequest): UserResponse = dbQuery {
//        val user = Users.selectAll()
//            .where { Users.loginId eq request.loginId }
//            .firstOrNull()?.let { row ->
//                val password = row[Users.password]
//                if (BCrypt.checkpw(request.password, password)) {
//                    UserMapper.toUserResponse(row)
//                } else null
//            } ?: throw ValidationException(CustomError.USER_FAIL_LOGIN)
//
//        user

        val user = Users.select(Users.columns)
            .where { Users.loginId eq request.loginId }
            .firstOrNull()

        if (user == null) {
            throw ValidationException(ErrorCode.USER_FAIL_LOGIN, "존재하지 않는 아이디입니다")
        }

        val password = user[Users.password]
        if (!BCrypt.checkpw(request.password, password)) {
            throw ValidationException(ErrorCode.USER_FAIL_LOGIN, "비밀번호가 일치하지 않습니다")
        }

        UserMapper.toUserResponse(user)
    }

    // 사용자 정보 조회
    override suspend fun getById(id: Long): UserResponse = dbQuery {
        Users.select(Users.columns)
            .where { Users.id eq id }
            .map { UserMapper.toUserResponse(it) }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)
    }

    // 사용자 정보 수정
    override suspend fun update(id: Long, request: UserUpdateRequest): UserResponse = dbQuery {
        validateUserUpdate(request)

        // 기존 프로필 사진 경로 가져오기
        val existingProfileImagePath = Users.select(Users.profileImagePath)
            .where { Users.id eq id }
            .map { it[Users.profileImagePath] }
            .firstOrNull()

        // 프로필 이미지가 변경된 경우
        request.profileImagePath?.let { newProfileImage ->

            // 기존 프로필 사진 파일 삭제
            existingProfileImagePath?.let { oldProfileImage ->
                val filePath = oldProfileImage.replace(baseUrl, "$uploadPath${File.separator}")
                println("Deleting file: $filePath")
                deleteFile(filePath)
            }

            // 작성자로 작성한 게시물 업데이트
            Boards.update({ Boards.createUserId eq id }) {
                it[createUserProfileImagePath] = newProfileImage
            }

            // 수정자로 수정한 게시물 업데이트
            Boards.update({ Boards.updateUserId eq id }) {
                it [updateUserProfileImagePath] = newProfileImage
            }

            // 댓글의 프로필 이미지 업데이트
            Comments.update({ Comments.createUserId eq id }) {
                it[profileImageUrl] = newProfileImage
            }
        }

        Users.update({ Users.id eq id }) {
            it[userName] = request.userName
            it[extraUserInfo] = request.extraUserInfo
            it[profileImagePath] = request.profileImagePath
            it[updatedAt] = LocalDateTime.now()
        }

        getById(id)
    }

    // 회원가입 데이터 검증
    private fun validateUserCreate(request: UserCreateRequest) {
        if (!Validation.validateLoginId(request.loginId)) {
            throw ValidationException(ErrorCode.INVALID_PARAMETER)
        }
//        if (!Validation.validatePassword(request.password)) {
//            throw ValidationException(CustomError.INVALID_PARAMETER)
//        }
        if (!Validation.validateName(request.userName)) {
            throw ValidationException(ErrorCode.INVALID_PARAMETER)
        }
    }

    // 사용자 정보 수정 데이터 검증
    private fun validateUserUpdate(request: UserUpdateRequest) {
        if (!Validation.validateName(request.userName)) {
            throw ValidationException(ErrorCode.INVALID_PARAMETER)
        }
    }

    private fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            val isDeleted = file.delete()
            if (!isDeleted) {
                println("Failed to delete file: $filePath")
            }
        } else {
            println("File does not exist: $filePath")
        }
    }
}
*/