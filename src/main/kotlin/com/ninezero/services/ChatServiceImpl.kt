package com.ninezero.services

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.UpdateOptions
import com.mongodb.reactivestreams.client.MongoDatabase
import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.*
import com.ninezero.models.error.ErrorCode
import com.ninezero.models.tables.Users
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.types.ObjectId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class ChatServiceImpl(
    database: MongoDatabase,
    private val notificationService: NotificationService
) : ChatService {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val messagesCollection = database.getCollection("messages")
    private val roomsCollection = database.getCollection("chat_rooms")
    private val members = ConcurrentHashMap<Long, MutableList<WebSocketSession>>()

    // DB 트랜잭션 처리
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(IO) { block() }

    // 채팅방 생성
    override suspend fun createRoom(request: CreateChatRoomRequest, creatorId: Long): String {
        val allParticipantIds = request.participantIds + creatorId

        if (request.participantIds.contains(creatorId)) {
            throw ValidationException(ErrorCode.INVALID_PARAMETER, "참가자 목록에 자신을 포함할 수 없습니다")
        }

        // 이미 존재하는 채팅방 확인
        val existingRoom = roomsCollection.find(
            Filters.and(
                Filters.all("participants.userId", allParticipantIds),
                Filters.size("participants", allParticipantIds.size),
                Filters.eq("status", ParticipantStatus.ACTIVE.name)
            )
        ).asFlow().firstOrNull()

        // 기존 채팅방 반환
        if (existingRoom != null) {
            roomsCollection.updateMany(
                Filters.eq("_id", existingRoom.getObjectId("_id")),
                Document("\$set", Document()
                    .append("participants.$[elem].status", ParticipantStatus.ACTIVE.name)
                    .append("participants.$[elem].unreadCount", 0)),
                UpdateOptions().arrayFilters(listOf(
                    Document("elem.status", ParticipantStatus.LEFT.name)
                ))
            ).awaitFirst()
            return existingRoom.getObjectId("_id").toString()
        }

        // 참가자 정보 조회
        val participants = dbQuery {
            Users.selectAll()
                .where { Users.id inList allParticipantIds }
                .map { row ->
                    Document()
                        .append("userId", row[Users.id].value)
                        .append("userLoginId", row[Users.loginId])
                        .append("userName", row[Users.userName])
                        .append("profileImagePath", row[Users.profileImagePath])
                        .append("status", ParticipantStatus.ACTIVE.name)
                        .append("unreadCount", 0)
                        .append("lastReadMessageId", null)
                        .append("leaveTimestamp", null)
                }
                .toList()
        }

        if (participants.size != request.participantIds.size + 1) {
            throw ValidationException(ErrorCode.NOT_FOUND_USER)
        }

        // 새 채팅방 생성
        val room = Document()
            .append("name", request.name)
            .append("participants", participants)
            .append("status", ParticipantStatus.ACTIVE.name)
            .append("createdAt", LocalDateTime.now().toString())
            .append("lastMessageAt", LocalDateTime.now().toString())
            .append("messageCount", 0)

        val result = roomsCollection.insertOne(room).awaitFirst()
        return result.insertedId?.asObjectId()?.value?.toString()
            ?: throw ValidationException(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    // 기존 채팅방 유무 확인
    override suspend fun checkExistingRoom(userId: Long, otherUserId: Long): String? {
        return roomsCollection.find(
            Filters.and(
                Filters.all("participants.userId", listOf(userId, otherUserId)),
                Filters.size("participants", 2),
                Filters.eq("status", ParticipantStatus.ACTIVE.name)
            )
        ).asFlow().firstOrNull()?.getObjectId("_id")?.toString()
    }

    // 채팅방 목록 조회
    override suspend fun getRooms(userId: Long, page: Int, size: Int): List<ChatRoomResponse> {
        return roomsCollection.find(
            Filters.and(
                Filters.elemMatch("participants",
                    Filters.and(
                        Filters.eq("userId", userId),
                        Filters.eq("status", ParticipantStatus.ACTIVE.name)
                    )
                ),
                Filters.eq("status", ParticipantStatus.ACTIVE.name)
            )
        )
            .sort(Sorts.descending("lastMessageAt"))
            .skip((page - 1) * size)
            .limit(size)
            .asFlow()
            .toList()
            .map { room ->
                val participant = room.getList("participants", Document::class.java)
                    .find { it.getLong("userId") == userId }
                val leaveTimestamp = participant?.getString("leaveTimestamp")?.let {
                    LocalDateTime.parse(it)
                }

                val lastMessage = messagesCollection.find(
                    Filters.and(
                        Filters.eq("roomId", room.getObjectId("_id").toString()),
                        when (leaveTimestamp) {
                            null -> Filters.exists("createdAt")
                            else -> Filters.gt("createdAt", leaveTimestamp.toString())
                        }
                    )
                ).sort(Sorts.descending("createdAt"))
                    .limit(1)
                    .asFlow()
                    .firstOrNull()

                ChatRoomResponse(
                    id = room.getObjectId("_id").toString(),
                    name = room.getString("name"),
                    participants = room.getList("participants", Document::class.java).map {
                        ChatParticipant(
                            userId = it.getLong("userId"),
                            userLoginId = it.getString("userLoginId"),
                            userName = it.getString("userName"),
                            profileImagePath = it.getString("profileImagePath"),
                            status = ParticipantStatus.valueOf(it.getString("status")),
                            unreadCount = it.getInteger("unreadCount"),
                            lastReadMessageId = it.getString("lastReadMessageId"),
                            leaveTimestamp = it.getString("leaveTimestamp")?.let { timestamp ->
                                LocalDateTime.parse(timestamp)
                            }
                        )
                    },
                    lastMessage = lastMessage?.let {
                        ChatMessage(
                            content = it.getString("content"),
                            senderId = it.getLong("senderId"),
                            senderName = it.getString("senderName"),
                            roomId = it.getString("roomId"),
                            id = it.getObjectId("_id").toString(),
                            createdAt = LocalDateTime.parse(it.getString("createdAt")),
                            leaveTimestamp = it.getString("leaveTimestamp")?.let { timestamp ->
                                LocalDateTime.parse(timestamp)
                            }
                        )
                    },
                    messageCount = room.getInteger("messageCount"),
                    createdAt = LocalDateTime.parse(room.getString("createdAt"))
                )
            }
    }

    // 채팅 메시지 목록 조회
    override suspend fun getMessages(userId: Long, roomId: String, page: Int, size: Int): List<ChatMessage> {
        // 사용자의 leaveTimestamp 조회
        val room = roomsCollection.find(
            Filters.eq("_id", ObjectId(roomId))
        ).asFlow().firstOrNull()

        val participant = room?.getList("participants", Document::class.java)
            ?.find { it.getLong("userId") == userId }

        val leaveTimestamp = participant?.getString("leaveTimestamp")

        return messagesCollection.find(
            Filters.and(
                Filters.eq("roomId", roomId),
                when (leaveTimestamp) {
                    null -> Filters.exists("createdAt")
                    else -> Filters.gt("createdAt", leaveTimestamp)
                }
            )
        )
            .sort(Sorts.descending("createdAt"))
            .skip((page - 1) * size)
            .limit(size)
            .asFlow()
            .toList()
            .map { doc ->
                ChatMessage(
                    content = doc.getString("content"),
                    senderId = doc.getLong("senderId"),
                    senderName = doc.getString("senderName"),
                    roomId = doc.getString("roomId"),
                    id = doc.getObjectId("_id").toString(),
                    createdAt = LocalDateTime.parse(doc.getString("createdAt")),
                    leaveTimestamp = doc.getString("leaveTimestamp")?.let {
                        LocalDateTime.parse(it)
                    }
                )
            }
    }

    // 웹소켓 연결
    override suspend fun connect(session: WebSocketSession, chatSession: ChatSession) {
        try {
            val userSessions = members.getOrPut(chatSession.userId) { CopyOnWriteArrayList() }
            userSessions.add(session)
        } catch (e: Exception) {
            logger.error("Failed to establish chat connection", e)
            throw ValidationException(ErrorCode.CHAT_CONNECTION_ERROR)
        }
    }

    // 웹소켓 연결 해제
    override suspend fun disconnect(userId: Long) {
        members[userId]?.forEach { it.close() }
        members.remove(userId)
    }

    // 메시지 전송
    override suspend fun sendMessage(chatSession: ChatSession, message: ChatMessageRequest) {
        val roomId = message.roomId ?: run {
            createRoom(CreateChatRoomRequest(
                name = "제목없음",
                participantIds = listOf(message.otherUserId!!)
            ), chatSession.userId)
        }

        // 채팅방 찾기
        val room = roomsCollection.find(
            Filters.eq("_id", ObjectId(roomId))
        ).asFlow().firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_CHAT_ROOM)

        // 채팅방의 특정 참가자가 나간 상태면 재활성화
        val participant = room.getList("participants", Document::class.java)
            .find { it.getLong("userId") == chatSession.userId }

        if (participant?.getString("status") == ParticipantStatus.LEFT.name) {
            roomsCollection.updateOne(
                Filters.and(
                    Filters.eq("_id", ObjectId(roomId)),
                    Filters.elemMatch("participants",
                        Filters.eq("userId", chatSession.userId)
                    )
                ),
                Document("\$set", Document()
                    .append("participants.$.status", ParticipantStatus.ACTIVE.name)
                    .append("participants.$.unreadCount", 0))
            ).awaitFirst()
        }

        val roomExists = roomsCollection.find(
            Filters.and(
                Filters.eq("_id", ObjectId(roomId)),
                Filters.elemMatch("participants",
                    Filters.and(
                        Filters.eq("userId", chatSession.userId),
                        Filters.eq("status", ParticipantStatus.ACTIVE.name)
                    )
                ),
                Filters.eq("status", ParticipantStatus.ACTIVE.name)
            )
        ).asFlow().firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_CHAT_ROOM)

        val messageDoc = Document()
            .append("content", message.content)
            .append("senderId", chatSession.userId)
            .append("senderName", chatSession.userName)
            .append("roomId", roomId)
            .append("createdAt", LocalDateTime.now().toString())
            .append("leaveTimestamp", null)

        messagesCollection.insertOne(messageDoc).awaitFirst()

        // 채팅방 정보 업데이트
        roomsCollection.updateOne(
            Filters.eq("_id", ObjectId(roomId)),
            Document()
                .append("\$set", Document().append("lastMessageAt", LocalDateTime.now().toString()))
                .append("\$inc", Document().append("messageCount", 1))
        ).awaitFirst()

        // 다른 참가자들의 안 읽은 메시지 수 증가
        roomsCollection.updateMany(
            Filters.and(
                Filters.eq("_id", ObjectId(roomId)),
                Filters.elemMatch("participants",
                    Filters.and(
                        Filters.ne("userId", chatSession.userId),
                        Filters.eq("status", ParticipantStatus.ACTIVE.name)
                    )
                )
            ),
            Document("\$inc", Document().append("participants.$.unreadCount", 1))
        ).awaitFirst()

        val chatMessage = ChatMessage(
            content = message.content,
            senderId = chatSession.userId,
            senderName = chatSession.userName,
            roomId = roomId,
            id = messageDoc.getObjectId("_id").toString(),
            createdAt = LocalDateTime.parse(LocalDateTime.now().toString()),
            leaveTimestamp = null
        )

        val messageJson = Json.encodeToString(chatMessage)
        val activeParticipants = roomExists.getList("participants", Document::class.java)
            .filter { it.getString("status") == ParticipantStatus.ACTIVE.name }

        // 메시지를 웹소켓으로 전송하고 오프라인 사용자에게는 FCM 알림 전송
        activeParticipants.forEach { roomParticipant ->
            val participantUserId = roomParticipant.getLong("userId")
            if (participantUserId != chatSession.userId) {
                val hasActiveWebSocket = members[participantUserId]?.isNotEmpty() == true

                // 웹소켓 연결이 있으면 메시지 전송
                members[participantUserId]?.forEach { socket ->
                    try {
                        socket.send(Frame.Text(messageJson))
                    } catch (e: Exception) {
                        logger.error("Error sending message to user $participantUserId", e)
                    }
                }

                // 웹소켓 연결이 없는 사용자에게는 FCM 푸시 알림 전송
                if (!hasActiveWebSocket) {
                    val senderInfo = roomExists.getList("participants", Document::class.java)
                        .find { it.getLong("userId") == chatSession.userId }

                    val notification = FCMNotification(
                        type = "chat",
                        title = chatSession.userName,
                        body = message.content.take(100),
                        roomId = roomId,
                        senderId = chatSession.userId,
                        senderName = chatSession.userName,
                        senderLoginId = senderInfo?.getString("userLoginId"),
                        senderProfileImagePath = senderInfo?.getString("profileImagePath")
                    )

                    notificationService.sendNotificationToUser(participantUserId, notification)
                }
            }
        }
    }

    // 메시지 읽음 처리
    override suspend fun markAsRead(userId: Long, roomId: String, messageId: String) {
        roomsCollection.updateOne(
            Filters.and(
                Filters.eq("_id", ObjectId(roomId)),
                Filters.elemMatch("participants", Filters.eq("userId", userId))
            ),
            Document("\$set", Document()
                .append("participants.$.unreadCount", 0)
                .append("participants.$.lastReadMessageId", messageId))
        ).awaitFirst()
    }

    // 채팅방 나가기
    override suspend fun leaveRoom(userId: Long, roomId: String) {
        val leaveTimestamp = LocalDateTime.now()

        // 채팅방 찾기
        val room = roomsCollection.find(
            Filters.eq("_id", ObjectId(roomId))
        ).asFlow().firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_CHAT_ROOM)

        // 현재 ACTIVE 상태인 참가자 수 확인
        val activeParticipants = room.getList("participants", Document::class.java)
            .count { it.getString("status") == ParticipantStatus.ACTIVE.name }

        if (activeParticipants == 1) {
            // 채팅방 삭제
            roomsCollection.deleteOne(
                Filters.eq("_id", ObjectId(roomId))
            ).awaitFirst()

            // 관련 메시지들 삭제
            messagesCollection.deleteMany(
                Filters.eq("roomId", roomId)
            ).awaitFirst()
        } else {
            roomsCollection.updateOne(
                Filters.and(
                    Filters.eq("_id", ObjectId(roomId)),
                    Filters.elemMatch("participants", Filters.eq("userId", userId))
                ),
                Document("\$set", Document()
                    .append("participants.$.status", ParticipantStatus.LEFT.name)
                    .append("participants.$.leaveTimestamp", leaveTimestamp.toString()))
            ).awaitFirst()

            messagesCollection.updateMany(
                Filters.and(
                    Filters.eq("roomId", roomId),
                    Filters.lte("createdAt", leaveTimestamp.toString())
                ),
                Document("\$set", Document("leaveTimestamp", leaveTimestamp.toString()))
            ).awaitFirst()
        }
    }
}