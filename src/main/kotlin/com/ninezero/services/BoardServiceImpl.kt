package com.ninezero.services

import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.*
import com.ninezero.models.error.ErrorCode
import com.ninezero.models.mappers.BoardMapper
import com.ninezero.models.mappers.CommentMapper
import com.ninezero.models.mappers.UserMapper
import com.ninezero.models.tables.*
import com.ninezero.utils.FileConfig
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BoardServiceImpl(
    private val fileService: FileService,
    private val notificationService: NotificationService
) : BoardService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // DB 트랜잭션 처리
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(IO) { block() }

    // 사용자 정보 조회
    private suspend fun getUser(userId: Long): User = dbQuery {
        Users.select(Users.columns)
            .where { Users.id eq userId }
            .map { UserMapper.toUser(it) }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)
    }

    // 게시글 정보 조회
    private suspend fun getBoard(boardId: Long, requesterId: Long): BoardResponse = dbQuery {
        val board = Boards.select(Boards.columns)
            .where { Boards.id eq boardId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_BOARD)

        val isLiked = LikeBoards
            .select(LikeBoards.boardId)
            .where { LikeBoards.boardId eq boardId and (LikeBoards.createUserId eq requesterId) }
            .firstOrNull() != null

        BoardMapper.toBoardResponse(board, isLiked, requesterId)
    }

    // 게시글 소유자 확인
    private suspend fun checkBoardOwner(board: BoardResponse, requesterId: Long) {
        if (board.createUserId != requesterId) {
            throw ValidationException(ErrorCode.INVALID_ACCESS_TO_BOARD)
        }
    }

    // 게시글 작성
    override suspend fun create(request: BoardRequest, requesterId: Long): Long = dbQuery {
        val user = getUser(requesterId)

        Boards.insert {
            it[title] = request.title
            it[content] = request.content
            it[createUserId] = requesterId
            it[createUserName] = user.userName
            it[createUserProfileImagePath] = user.profileImagePath
            it[updateUserId] = requesterId
            it[updateUserName] = user.userName
            it[updateUserProfileImagePath] = user.profileImagePath
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }[Boards.id].value
    }

    // 게시글 목록 조회
    override suspend fun getList(page: Int, size: Int, requesterId: Long): List<BoardResponse> = dbQuery {
        val boards = Boards.select(Boards.columns)
            .orderBy(Boards.id to SortOrder.DESC)
            .limit(size)
            .offset(start = ((page - 1) * size).toLong())
            .toList()

        if (boards.isEmpty()) return@dbQuery emptyList()

        // 사용자가 좋아요를 눌렀는지 확인
        val boardIds = boards.map { it[Boards.id].value }
        val isLikedMap = LikeBoards
            .select(LikeBoards.boardId)
            .where { LikeBoards.boardId inList boardIds and (LikeBoards.createUserId eq requesterId) }
            .associate { it[LikeBoards.boardId] to true }

        BoardMapper.toBoardResponseList(boards, isLikedMap, requesterId)
    }

    // 내 게시글 목록 조회
    override suspend fun getMyList(page: Int, size: Int, requesterId: Long): List<BoardResponse> = dbQuery {
        val boards = Boards.select(Boards.columns)
            .where { Boards.createUserId eq requesterId }
            .orderBy(Boards.id to SortOrder.DESC)
            .limit(size)
            .offset(start = ((page - 1) * size).toLong())
            .toList()

        if (boards.isEmpty()) return@dbQuery emptyList()

        val boardIds = boards.map { it[Boards.id].value }
        val isLikedMap = LikeBoards
            .select(LikeBoards.boardId)
            .where { LikeBoards.boardId inList boardIds and (LikeBoards.createUserId eq requesterId) }
            .associate { it[LikeBoards.boardId] to true }

        BoardMapper.toBoardResponseList(boards, isLikedMap, requesterId)
    }

    // 저장된 게시글 목록 조회
    override suspend fun getSavedList(page: Int, size: Int, requesterId: Long): List<BoardResponse> = dbQuery {
        val savedBoardIds = SavedBoards
            .select(SavedBoards.boardId)
            .where { SavedBoards.createUserId eq requesterId }
            .map { it[SavedBoards.boardId] }

        if (savedBoardIds.isEmpty()) return@dbQuery emptyList()

        val boards = Boards.select(Boards.columns)
            .where { Boards.id inList savedBoardIds }
            .orderBy(Boards.id to SortOrder.DESC)
            .limit(size)
            .offset(start = ((page - 1) * size).toLong())
            .toList()

        val boardIds = boards.map { it[Boards.id].value }
        val isLikedMap = LikeBoards
            .select(LikeBoards.boardId)
            .where { LikeBoards.boardId inList boardIds and (LikeBoards.createUserId eq requesterId) }
            .associate { it[LikeBoards.boardId] to true }

        BoardMapper.toBoardResponseList(boards, isLikedMap, requesterId)
    }

    // 특정 사용자 게시글 목록 조회
    override suspend fun getListById(userId: Long, page: Int, size: Int, requesterId: Long): List<BoardResponse> = dbQuery {
        val boards = Boards.select(Boards.columns)
            .where { Boards.createUserId eq userId }
            .orderBy(Boards.id to SortOrder.DESC)
            .limit(size)
            .offset(start = ((page - 1) * size).toLong())
            .toList()

        if (boards.isEmpty()) return@dbQuery emptyList()

        val boardIds = boards.map { it[Boards.id].value }
        val isLikedMap = LikeBoards
            .select(LikeBoards.boardId)
            .where { LikeBoards.boardId inList boardIds and (LikeBoards.createUserId eq requesterId) }
            .associate { it[LikeBoards.boardId] to true }

        BoardMapper.toBoardResponseList(boards, isLikedMap, requesterId)
    }

    // 게시글 수정
    override suspend fun update(id: Long, request: BoardRequest, requesterId: Long): Long = dbQuery {
        val user = getUser(requesterId)

        val board = getBoard(id, requesterId)
        checkBoardOwner(board, requesterId)

        Boards.update({ Boards.id eq id }) {
            it[title] = request.title
            it[content] = request.content
            it[updateUserId] = requesterId
            it[updateUserName] = user.userName
            it[updateUserProfileImagePath] = user.profileImagePath
            it[updatedAt] = LocalDateTime.now()
        }
        id
    }

    // 게시글 삭제
    override suspend fun delete(id: Long, requesterId: Long): Long = dbQuery {
        val board = getBoard(id, requesterId)
        checkBoardOwner(board, requesterId)

        // 이미지 파일 삭제
        val imageIds = extractImageIds(board.content)
        imageIds.forEach { imageId ->
            try {
                FileConfig.extractFileId(imageId)?.let {
                    fileService.deleteFile(it)
                }
            } catch (e: Exception) {
                logger.error("Failed to delete file: $imageId", e)
            }
        }

        // LikeBoards Table 에서 관련 레코드 삭제
        LikeBoards.deleteWhere { LikeBoards.boardId eq id }

        val deleted = Boards.deleteWhere { Boards.id eq id }
        if (deleted == 0) throw ValidationException(ErrorCode.NOT_FOUND_BOARD)
        id
    }

    // 댓글 조회
    override suspend fun getComments(boardId: Long, page: Int, size: Int): List<CommentResponse> = dbQuery {
        // 게시글 존재 여부 확인
        Boards.selectAll()
            .where { Boards.id eq boardId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_BOARD)

        Comments
            .selectAll()
            .where { (Comments.boardId eq boardId) and Comments.parentId.isNull() }
            .orderBy(Comments.createdAt to SortOrder.ASC)
            .limit(size)
            .offset(((page - 1) * size).toLong())
            .map { CommentMapper.toCommentResponse(it) }
    }

    // 대댓글 조회
    override suspend fun getReplies(boardId: Long, parentId: Long): List<CommentResponse> = dbQuery {
        // 게시글과 부모 댓글 존재 여부 확인
        Boards.selectAll()
            .where { Boards.id eq boardId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_BOARD)

        // 부모 댓글 조회
        val parentComment = Comments.selectAll()
            .where { Comments.id eq parentId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_COMMENT)

        val parentUserName = parentComment[Comments.createUserName]

        // 대댓글 전체 조회
        Comments
            .selectAll()
            .where {
                (Comments.boardId eq boardId) and (Comments.parentId eq parentId)
            }
            .orderBy(Comments.createdAt to SortOrder.ASC)
            .map { CommentMapper.toCommentResponse(it, parentUserName) }
    }

    // 댓글 작성
    override suspend fun createComment(boardId: Long, request: CommentRequest, requesterId: Long): Long = dbQuery {
        // 게시글 존재 여부 확인
        val board = Boards.select(Boards.columns)
            .where { Boards.id eq boardId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_BOARD)

        // 부모 댓글 확인 및 유효성 검사
        request.parentId?.let { pid ->
            val parentComment = Comments.selectAll()
                .where { Comments.id eq pid }
                .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_COMMENT)

            // 2단계 구조 유지: 이미 대댓글인 댓글에는 답글을 달 수 없음
            if (parentComment[Comments.depth] > 0) {
                throw ValidationException(ErrorCode.INVALID_REPLY_DEPTH)
            }
        }

        val mentionedUserIds = request.mentionedUserIds?.let {
            Json.encodeToString(it)
        }

        var replyToUserName: String? = null
        request.replyToCommentId?.let { replyId ->
            val replyToComment = Comments.selectAll()
                .where { Comments.id eq replyId }
                .firstOrNull()

            replyToUserName = replyToComment?.get(Comments.createUserName)
        }

        val user = getUser(requesterId)

        val commentId = Comments.insert {
            it[comment] = request.comment
            it[this.boardId] = boardId
            it[parentId] = request.parentId
            it[depth] = if (request.parentId != null) 1 else 0
            it[this.mentionedUserIds] = mentionedUserIds
            it[replyToCommentId] = request.replyToCommentId
            it[this.replyToUserName] = replyToUserName
            it[createUserId] = requesterId
            it[createUserName] = user.userName
            it[profileImageUrl] = user.profileImagePath
            it[createdAt] = LocalDateTime.now()
        }[Comments.id].value

        // 부모 댓글이 있는 경우 replyCount 증가
        request.parentId?.let { pid ->
            Comments.update({ Comments.id eq pid }) {
                it[replyCount] = Comments.replyCount.plus(1)
            }
        }

        // 댓글이 달린 게시글의 작성자에게 알림
        val boardOwnerId = board[Boards.createUserId]

        // 본인 게시글에 본인이 댓글 달았을 시 알림 X
        if (boardOwnerId != requesterId) {
            val notification = FCMNotification(
                type = "comment",
                title = "새 댓글",
                body = "${user.userName}님이 회원님의 게시글에 댓글을 남겼습니다: ${request.comment.take(30)}${if(request.comment.length > 30) "..." else ""}",
                boardId = boardId,
                commentId = commentId,
                userId = requesterId
            )

            notificationService.sendNotificationToUser(boardOwnerId, notification)
        }

        request.replyToCommentId?.let { replyToId ->
            val replyToComment = Comments.select(Comments.createUserId)
                .where { Comments.id eq replyToId }
                .firstOrNull()

            replyToComment?.let {
                val replyToUserId = it[Comments.createUserId]
                if (replyToUserId != requesterId && replyToUserId != boardOwnerId) {
                    val notification = FCMNotification(
                        type = "reply",
                        title = "새 답글",
                        body = "${user.userName}님이 회원님의 댓글에 답글을 남겼습니다: ${request.comment.take(30)}${if(request.comment.length > 30) "..." else ""}",
                        boardId = boardId,
                        commentId = commentId,
                        userId = requesterId
                    )
                    notificationService.sendNotificationToUser(replyToUserId, notification)
                }
            }
        }

        commentId
    }

    // 댓글 삭제
    override suspend fun deleteComment(boardId: Long, commentId: Long, requesterId: Long): Long = dbQuery {
        // 게시글 조회
        val board = Boards.select(Boards.createUserId)
            .where { Boards.id eq boardId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_BOARD)

        val comment = Comments.selectAll()
            .where { Comments.id eq commentId and (Comments.boardId eq boardId) }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_COMMENT)

        // 댓글 작성자이거나 게시글 작성자인 경우에만 삭제 가능
        if (comment[Comments.createUserId] != requesterId && board[Boards.createUserId] != requesterId) {
            throw ValidationException(ErrorCode.INVALID_ACCESS_TO_COMMENT)
        }

        val parentId = comment[Comments.parentId]

        // 대댓글이 있는 경우 모두 삭제
        if (comment[Comments.replyCount] > 0) {
            Comments.deleteWhere {
                Comments.parentId eq commentId
            }
        }

        val deleted = Comments.deleteWhere {
            Comments.id eq commentId
        }

        if (deleted > 0 && parentId != null) {
            // 부모 댓글의 replyCount 감소
            Comments.update({ Comments.id eq parentId }) {
                it[replyCount] = Comments.replyCount.minus(1)
            }
        }

        commentId
    }

    // 게시글 좋아요
    override suspend fun like(boardId: Long, requesterId: Long): Long = dbQuery {
        // 본인 게시글인지 확인
        val board = Boards.select(Boards.createUserId)
            .where { Boards.id eq boardId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_BOARD)

//        if (board[Boards.createUserId] == requesterId) {
//            throw ValidationException(ErrorCode.CANNOT_LIKE_OWN_BOARD)
//        }

        val exists = LikeBoards
            .select(LikeBoards.id)
            .where { LikeBoards.boardId eq boardId and (LikeBoards.createUserId eq requesterId) }
            .firstOrNull()

        if (exists != null) throw ValidationException(ErrorCode.ALREADY_LIKED)

        val likeId = LikeBoards.insert {
            it[this.boardId] = boardId
            it[createUserId] = requesterId
            it[createdAt] = LocalDateTime.now()
        }[LikeBoards.id].value

        // 게시글 작성자에게 알림
        val boardOwnerId = board[Boards.createUserId]

        // 본인 게시글에 본인이 좋아요 했을 시 알림 X
        if (boardOwnerId != requesterId) {
            val user = getUser(requesterId)

            val notification = FCMNotification(
                type = "like",
                title = "새 좋아요",
                body = "${user.userName}님이 회원님의 게시글을 좋아합니다.",
                boardId = boardId,
                userId = requesterId
            )

            notificationService.sendNotificationToUser(boardOwnerId, notification)
        }

        likeId
    }

    // 게시글 좋아요 취소
    override suspend fun unlike(boardId: Long, requesterId: Long): Long = dbQuery {
        val deleted = LikeBoards.deleteWhere {
            LikeBoards.boardId eq boardId and (LikeBoards.createUserId eq requesterId)
        }
        if (deleted == 0) throw ValidationException(ErrorCode.ALREADY_CANCELED)
        boardId
    }

    // 게시글 저장
    override suspend fun saveBoard(boardId: Long, requesterId: Long): Long = dbQuery {
        val board = getBoard(boardId, requesterId)

        // 본인 게시글 저장 방지
        if (board.createUserId == requesterId) {
            throw ValidationException(ErrorCode.CANNOT_SAVE_OWN_BOARD)
        }

        val exists = SavedBoards
            .select(SavedBoards.id)
            .where { SavedBoards.boardId eq boardId and (SavedBoards.createUserId eq requesterId) }
            .firstOrNull()

        if (exists != null) throw ValidationException(ErrorCode.ALREADY_SAVED)

        SavedBoards.insert {
            it[this.boardId] = boardId
            it[createUserId] = requesterId
            it[createdAt] = LocalDateTime.now()
        }[SavedBoards.id].value
    }

    // 게시글 저장 취소
    override suspend fun unsaveBoard(boardId: Long, requesterId: Long): Long = dbQuery {
        val deleted = SavedBoards.deleteWhere {
            SavedBoards.boardId eq boardId and (SavedBoards.createUserId eq requesterId)
        }
        if (deleted == 0) throw ValidationException(ErrorCode.ALREADY_UNSAVED)
        boardId
    }

    // MongoDB ObjectId 추출
    private fun extractImageIds(content: String): List<String> {
        return try {
            val json = Json.parseToJsonElement(content).jsonObject
            json["images"]?.let { Json.decodeFromJsonElement<List<String>>(it) } ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to extract image IDs from content", e)
            emptyList()
        }
    }
}

/**
class BoardServiceImpl(
    private val config: ApplicationConfig
) : BoardService {
    private val uploadPath = File(config.property("file.uploadPath").getString()).absolutePath
    private val baseUrl = config.property("file.baseUrl").getString()

    // DB 트랜잭션 처리
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(IO) { block() }

    // 사용자 정보 조회
    private suspend fun getUser(userId: Long): User = dbQuery {
        Users.select(Users.columns)
            .where { Users.id eq userId }
            .map { UserMapper.toUser(it) }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)
    }

    // 게시글 정보 조회
    private suspend fun getBoard(boardId: Long, requesterId: Long): BoardResponse = dbQuery {
        val board = Boards.select(Boards.columns)
            .where { Boards.id eq boardId }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_BOARD)

        val isLiked = LikeBoards
            .select(LikeBoards.boardId)
            .where { LikeBoards.boardId eq boardId and (LikeBoards.createUserId eq requesterId) }
            .firstOrNull() != null

        BoardMapper.toBoardResponse(board, isLiked)
    }

    // 게시글 소유자 확인
    private suspend fun checkBoardOwner(board: BoardResponse, requesterId: Long) {
        if (board.createUserId != requesterId) {
            throw ValidationException(ErrorCode.INVALID_ACCESS_TO_BOARD)
        }
    }

    // 게시글 작성
    override suspend fun create(request: BoardRequest, requesterId: Long): Long = dbQuery {
        val user = getUser(requesterId)

        Boards.insert {
            it[title] = request.title
            it[content] = request.content
            it[createUserId] = requesterId
            it[createUserName] = user.userName
            it[createUserProfileImagePath] = user.profileImagePath
            it[updateUserId] = requesterId
            it[updateUserName] = user.userName
            it[updateUserProfileImagePath] = user.profileImagePath
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }[Boards.id].value
    }

    // 게시글 목록 조회
    override suspend fun getList(page: Int, size: Int, requesterId: Long): List<BoardResponse> = dbQuery {
        val boards = Boards.select(Boards.columns)
            .orderBy(Boards.id to SortOrder.DESC)
            .limit(size)
            .offset(start = ((page - 1) * size).toLong())
            .toList()

        // 사용자가 좋아요를 눌렀는지 확인
        val boardIds = boards.map { it[Boards.id].value }
        val isLikedMap = LikeBoards
            .select(LikeBoards.boardId)
            .where { LikeBoards.boardId inList boardIds and (LikeBoards.createUserId eq requesterId) }
            .associate { it[LikeBoards.boardId] to true }

        BoardMapper.toBoardResponseList(boards, isLikedMap)
    }

    // 게시글 수정
    override suspend fun update(id: Long, request: BoardRequest, requesterId: Long): Long = dbQuery {
        val user = getUser(requesterId)

        val board = getBoard(id, requesterId)
        //checkBoardOwner(board, requesterId)

        Boards.update({ Boards.id eq id }) {
            it[title] = request.title
            it[content] = request.content
            it[updateUserId] = requesterId
            it[updateUserName] = user.userName
            it[updateUserProfileImagePath] = user.profileImagePath
            it[updatedAt] = LocalDateTime.now()
        }
        id
    }

    // 게시글 삭제
    override suspend fun delete(id: Long, requesterId: Long): Long = dbQuery {
        val board = getBoard(id, requesterId)
        //checkBoardOwner(board, requesterId)

        // 이미지 파일 삭제
        val imageUrls = extractImageUrls(board.content)
        imageUrls.forEach { imageUrl ->
            val filePath = imageUrl.replace(baseUrl, "$uploadPath${File.separator}")
            println("Deleting file: $filePath")
            deleteFile(filePath)
        }

        // LikeBoards Table 에서 관련 레코드 삭제
        LikeBoards.deleteWhere { LikeBoards.boardId eq id }

        val deleted = Boards.deleteWhere { Boards.id eq id }
        if (deleted == 0) throw ValidationException(ErrorCode.NOT_FOUND_BOARD)
        id
    }

    // 댓글 작성
    override suspend fun createComment(boardId: Long, request: CommentRequest, requesterId: Long): Long = dbQuery {
        val user = getUser(requesterId)

        Comments.insert {
            it[comment] = request.comment
            it[this.boardId] = boardId
            it[createUserId] = requesterId
            it[createUserName] = user.userName
            it[profileImageUrl] = user.profileImagePath
            it[createdAt] = LocalDateTime.now()
        }[Comments.id].value
    }

    // 댓글 삭제
    override suspend fun deleteComment(boardId: Long, commentId: Long): Long = dbQuery {
        val deleted = Comments.deleteWhere {
            Comments.id eq commentId and (Comments.boardId eq boardId)
        }
        if (deleted == 0) throw ValidationException(ErrorCode.NOT_FOUND_COMMENT)
        commentId
    }

    // 게시글 좋아요
    override suspend fun like(boardId: Long, requesterId: Long): Long = dbQuery {
        val exists = LikeBoards
            .select(LikeBoards.id)
            .where { LikeBoards.boardId eq boardId and (LikeBoards.createUserId eq requesterId) }
            .firstOrNull()

        if (exists != null) throw ValidationException(ErrorCode.ALREADY_LIKED)

        LikeBoards.insert {
            it[this.boardId] = boardId
            it[createUserId] = requesterId
            it[createdAt] = LocalDateTime.now()
        }[LikeBoards.id].value
    }

    // 게시글 좋아요 취소
    override suspend fun unlike(boardId: Long, requesterId: Long): Long = dbQuery {
        val deleted = LikeBoards.deleteWhere {
            LikeBoards.boardId eq boardId and (LikeBoards.createUserId eq requesterId)
        }
        if (deleted == 0) throw ValidationException(ErrorCode.ALREADY_CANCELED)
        1
    }

    // 이미지 파일 경로 추출
    private fun extractImageUrls(content: String): List<String> {
        return try {
            val json = Json.parseToJsonElement(content).jsonObject
            val imageUrls = json["images"]?.let { Json.decodeFromJsonElement<List<String>>(it) } ?: emptyList()
            println("Extracted image URLs: $imageUrls")
            imageUrls
        } catch (e: Exception) {
            println("Failed to extract image URLs: ${e.message}")
            emptyList()
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