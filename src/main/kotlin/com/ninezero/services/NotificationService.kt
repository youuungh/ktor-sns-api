package com.ninezero.services

import com.ninezero.models.dto.DeviceTokenRequest
import com.ninezero.models.dto.FCMNotification
import com.ninezero.models.dto.NotificationResponse

interface NotificationService {
    suspend fun registerDeviceToken(userId: Long, request: DeviceTokenRequest): Long
    suspend fun removeDeviceToken(userId: Long, token: String): Boolean
    suspend fun getActiveTokensByUserId(userId: Long): List<String>
    suspend fun sendNotificationToUser(userId: Long, notification: FCMNotification)
    suspend fun getNotifications(userId: Long, page: Int, size: Int): List<NotificationResponse>
    suspend fun saveNotification(notification: FCMNotification, userId: Long): Long
    suspend fun markAsRead(userId: Long, notificationId: Long): Boolean
    suspend fun hasUnreadNotifications(userId: Long): Boolean
    suspend fun deleteNotification(userId: Long, notificationId: Long): Boolean
    suspend fun deleteAllNotifications(userId: Long): Boolean
}