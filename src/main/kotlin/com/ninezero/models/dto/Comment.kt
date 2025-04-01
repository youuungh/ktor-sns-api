package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Comment(
    val id: Long = 0,
    val comment: String,
    val createdBy: User,
    val board: Board,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime?
)

@Serializable
data class CommentRequest(
    val comment: String,
    val parentId: Long?,
    val mentionedUserIds: List<Long>? = null,
    val replyToCommentId: Long? = null
)

@Serializable
data class CommentResponse(
    val id: Long,
    val comment: String,
    val parentId: Long?,
    val parentUserName: String?,
    val depth: Int,
    val replyCount: Int,
    val mentionedUserIds: List<Long>?,
    val replyToCommentId: Long?,
    val replyToUserName: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val createUserId: Long,
    val createUserName: String,
    val profileImageUrl: String?
)