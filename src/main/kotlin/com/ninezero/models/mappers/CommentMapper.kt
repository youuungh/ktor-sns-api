package com.ninezero.models.mappers

import com.ninezero.models.dto.CommentResponse
import com.ninezero.models.tables.Comments
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow

object CommentMapper {
    fun toCommentResponse(row: ResultRow, parentUserName: String? = null) = CommentResponse(
        id = row[Comments.id].value,
        comment = row[Comments.comment],
        parentId = row[Comments.parentId],
        parentUserName = parentUserName,
        depth = row[Comments.depth],
        mentionedUserIds = row[Comments.mentionedUserIds]?.let {
            Json.decodeFromString<List<Long>>(it)
        },
        replyCount = row[Comments.replyCount],
        replyToCommentId = row[Comments.replyToCommentId],
        replyToUserName = row[Comments.replyToUserName],
        createdAt = row[Comments.createdAt],
        createUserId = row[Comments.createUserId],
        createUserName = row[Comments.createUserName],
        profileImageUrl = row[Comments.profileImageUrl]
    )
}