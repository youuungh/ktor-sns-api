package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class DeviceTokenRequest(
    val token: String,
    val deviceInfo: String? = null
)

@Serializable
data class DeviceTokenResponse(
    val id: Long,
    val userId: Long,
    val token: String,
    val deviceInfo: String?,
    val isActive: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime
)

@Serializable
data class FCMNotification(
    val type: String,
    val title: String,
    val body: String,
    val boardId: Long? = null,
    val commentId: Long? = null,
    val userId: Long? = null,
    val roomId: String? = null,
    val senderId: Long? = null,
    val senderLoginId: String? = null,
    val senderName: String? = null,
    val senderProfileImagePath: String? = null
)