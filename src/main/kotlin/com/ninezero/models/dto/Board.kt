package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Board(
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdBy: User,
    val updatedBy: User,
    val comments: List<Comment> = emptyList(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null
)

@Serializable
data class BoardRequest(
    val title: String,
    val content: String
)

@Serializable
data class BoardResponse(
    val id: Long,
    val title: String,
    val content: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val createUserId: Long,
    val createUserName: String,
    val createUserProfileImagePath: String?,
    val updateUserId: Long,
    val updateUserName: String,
    val updateUserProfileImagePath: String?,
    val comments: List<CommentResponse>,
    val commentCount: Int,
    val likesCount: Int,
    val isLiked: Boolean,
    val isFollowing: Boolean,
    val isSaved: Boolean
)