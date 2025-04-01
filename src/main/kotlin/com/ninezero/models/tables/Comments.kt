package com.ninezero.models.tables

import org.jetbrains.exposed.sql.ReferenceOption

object Comments : BaseTable("comments") {
    val comment = varchar("comment", 500)
    val boardId = long("board_id").references(Boards.id, onDelete = ReferenceOption.CASCADE)
    val parentId = long("parent_id").references(id, onDelete = ReferenceOption.CASCADE).nullable()
    val depth = integer("depth").default(0)
    val mentionedUserIds = text("mentioned_user_ids").nullable()
    val replyCount = integer("reply_count").default(0)
    val replyToCommentId = long("reply_to_comment_id").references(id, onDelete = ReferenceOption.SET_NULL).nullable()
    val replyToUserName = varchar("reply_to_user_name", 50).nullable()
    val createUserId = long("create_user_id").references(Users.id)
    val createUserName = varchar("create_user_name", 50)
    val profileImageUrl = varchar("profile_image_url", 255).nullable()
}