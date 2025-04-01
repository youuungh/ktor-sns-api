package com.ninezero.services

import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.DeviceTokenRequest
import com.ninezero.models.dto.FCMNotification
import com.ninezero.models.dto.NotificationResponse
import com.ninezero.models.error.ErrorCode
import com.ninezero.models.mappers.NotificationMapper
import com.ninezero.models.tables.DeviceTokens
import com.ninezero.models.tables.Notifications
import com.ninezero.models.tables.Users
import kotlinx.coroutines.Dispatchers.IO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class NotificationServiceImpl(
    private val fcmService: FCMService
) : NotificationService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(IO) { block() }

    override suspend fun registerDeviceToken(userId: Long, request: DeviceTokenRequest): Long = dbQuery {
        // 사용자 존재 여부 확인
        Users.selectAll().where { Users.id eq userId }.firstOrNull()
            ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)

        // 기존 토큰 비활성화 (같은 디바이스)
        DeviceTokens.update({ DeviceTokens.userId eq userId and (DeviceTokens.token eq request.token) }) {
            it[isActive] = true
            it[updatedAt] = LocalDateTime.now()
        }

        // 새 토큰 추가 또는 중복 방지
        val existing = DeviceTokens.selectAll()
            .where { DeviceTokens.userId eq userId and (DeviceTokens.token eq request.token) }.firstOrNull()

        if (existing != null) {
            existing[DeviceTokens.id].value
        } else {
            DeviceTokens.insert {
                it[this.userId] = userId
                it[token] = request.token
                it[deviceInfo] = request.deviceInfo
                it[isActive] = true
                it[createdAt] = LocalDateTime.now()
                it[updatedAt] = LocalDateTime.now()
            }[DeviceTokens.id].value
        }
    }

    override suspend fun removeDeviceToken(userId: Long, token: String): Boolean = dbQuery {
        val updated = DeviceTokens.update({
            DeviceTokens.userId eq userId and (DeviceTokens.token eq token)
        }) {
            it[isActive] = false
            it[updatedAt] = LocalDateTime.now()
        }

        updated > 0
    }

    override suspend fun getActiveTokensByUserId(userId: Long): List<String> = dbQuery {
        DeviceTokens.selectAll().where { DeviceTokens.userId eq userId and DeviceTokens.isActive }.map { it[DeviceTokens.token] }
    }

    override suspend fun sendNotificationToUser(userId: Long, notification: FCMNotification) {
        try {
            // 푸쉬 알림
            val tokens = getActiveTokensByUserId(userId)
            if (tokens.isNotEmpty()) {
                fcmService.sendMulticast(tokens, notification)
            }

            saveNotification(notification, userId)
        } catch (e: Exception) {
            logger.error("Failed to send notification to user $userId: ${e.message}", e)
        }
    }

    override suspend fun getNotifications(userId: Long, page: Int, size: Int): List<NotificationResponse> = dbQuery {
        val notifications = Notifications
            .select(Notifications.columns)
            .where { Notifications.userId eq userId }
            .orderBy(Notifications.createdAt to SortOrder.DESC)
            .limit(size)
            .offset(((page - 1) * size).toLong())
            .toList()

        if (notifications.isEmpty()) return@dbQuery emptyList()

        // 발신자 정보 조회
        val senderIds = notifications.mapNotNull { it[Notifications.senderId] }.toSet()
        val sendersMap = if (senderIds.isNotEmpty()) {
            Users.select(Users.id, Users.userName, Users.loginId, Users.profileImagePath)
                .where { Users.id inList senderIds }
                .associate {
                    it[Users.id].value to Triple(it[Users.userName], it[Users.loginId], it[Users.profileImagePath]) // Include loginId
                }
        } else {
            emptyMap()
        }

        notifications.map { row ->
            val senderId = row[Notifications.senderId]
            val (senderName, senderLoginId, senderProfileImagePath) = sendersMap[senderId] ?: Triple(null, null, null)

            NotificationMapper.toNotificationResponse(
                row = row,
                senderName = senderName,
                senderLoginId = senderLoginId,
                senderProfileImagePath = senderProfileImagePath
            )
        }
    }

    override suspend fun saveNotification(notification: FCMNotification, userId: Long): Long = dbQuery {
        Notifications.insert {
            it[this.userId] = userId
            it[type] = notification.type
            it[body] = notification.body
            it[senderId] = if (notification.type == "chat") notification.senderId else notification.userId
            it[boardId] = notification.boardId
            it[commentId] = notification.commentId
            it[roomId] = notification.roomId
            it[isRead] = false
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }[Notifications.id].value
    }

    override suspend fun markAsRead(userId: Long, notificationId: Long): Boolean = dbQuery {
        val updated = Notifications.update({
            (Notifications.id eq notificationId) and (Notifications.userId eq userId)
        }) {
            it[isRead] = true
            it[updatedAt] = LocalDateTime.now()
        }

        updated > 0
    }

    override suspend fun hasUnreadNotifications(userId: Long): Boolean = dbQuery {
        !Notifications
            .select(Notifications.id)
            .where {
                (Notifications.userId eq userId) and
                        (Notifications.isRead eq false)
            }
            .limit(1)
            .empty()
    }

    override suspend fun deleteNotification(userId: Long, notificationId: Long): Boolean = dbQuery {
        val deleted = Notifications.deleteWhere {
            (Notifications.id eq notificationId) and (Notifications.userId eq userId)
        }

        deleted > 0
    }

    override suspend fun deleteAllNotifications(userId: Long): Boolean = dbQuery {
        val deleted = Notifications.deleteWhere { Notifications.userId eq userId }

        deleted > 0
    }
}