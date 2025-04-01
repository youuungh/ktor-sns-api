package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class NotificationResponse(
    val id: Long,
    val type: String,
    val body: String,
    val senderId: Long?,
    val senderLoginId: String?,
    val senderName: String?,
    val senderProfileImagePath: String?,
    val boardId: Long?,
    val commentId: Long?,
    val roomId: String?,
    val isRead: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
)