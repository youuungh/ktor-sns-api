package com.ninezero.models.mappers

import com.ninezero.models.dto.NotificationResponse
import com.ninezero.models.tables.Notifications
import org.jetbrains.exposed.sql.ResultRow

object NotificationMapper {
    fun toNotificationResponse(
        row: ResultRow,
        senderName: String? = null,
        senderLoginId: String? = null,
        senderProfileImagePath: String? = null
    ): NotificationResponse = NotificationResponse(
        id = row[Notifications.id].value,
        type = row[Notifications.type],
        body = row[Notifications.body],
        senderId = row[Notifications.senderId],
        senderName = senderName,
        senderLoginId = senderLoginId,
        senderProfileImagePath = senderProfileImagePath,
        boardId = row[Notifications.boardId],
        commentId = row[Notifications.commentId],
        roomId = row[Notifications.roomId],
        isRead = row[Notifications.isRead],
        createdAt = row[Notifications.createdAt]
    )
}