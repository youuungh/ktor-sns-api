package com.ninezero.models.tables

object Notifications : BaseTable("notifications") {
    val userId = long("user_id").references(Users.id)
    val type = varchar("type", 50)
    val body = text("body")
    val senderId = long("sender_id").references(Users.id).nullable()
    val boardId = long("board_id").references(Boards.id).nullable()
    val commentId = long("comment_id").nullable()
    val roomId = varchar("room_id", 255).nullable()
    val isRead = bool("is_read").default(false)
}