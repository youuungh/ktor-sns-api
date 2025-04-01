package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.LocalDateTime

enum class ParticipantStatus {
    ACTIVE, LEFT
}

@Serializable
data class ChatMessage(
    val content: String,
    val senderId: Long,
    val senderName: String,
    val roomId: String,
    @BsonId
    val id: String = ObjectId().toString(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val leaveTimestamp: LocalDateTime?
)

@Serializable
data class ChatParticipant(
    val userId: Long,
    val userLoginId: String,
    val userName: String,
    val profileImagePath: String?,
    val status: ParticipantStatus = ParticipantStatus.ACTIVE,
    val unreadCount: Int,
    val lastReadMessageId: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val leaveTimestamp: LocalDateTime?
)

@Serializable
data class ChatSession(
    val userId: Long,
    val userName: String,
    val sessionId: String
)

@Serializable
data class CreateChatRoomRequest(
    val name: String,
    val participantIds: List<Long>
)

@Serializable
data class ChatMessageRequest(
    val content: String,
    val roomId: String?,
    val otherUserId: Long?
)

@Serializable
data class ChatRoomResponse(
    val id: String,
    val name: String,
    val participants: List<ChatParticipant>,
    val lastMessage: ChatMessage?,
    val messageCount: Int,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
)