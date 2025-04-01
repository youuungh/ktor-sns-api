package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class User(
    val id: Long = 0,
    val loginId: String,
    val userName: String,
    val password: String = "",
    val extraUserInfo: String? = null,
    val profileImagePath: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null
)

@Serializable
data class UserResponse(
    val id: Long,
    val loginId: String,
    val userName: String,
    val extraUserInfo: String?,
    val profileImagePath: String?,
    val boardCount: Int,
    val followerCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime?
)

@Serializable
data class UserCreateRequest(
    val loginId: String,
    val userName: String,
    val password: String,
    val extraUserInfo: String? = null,
    val profileImagePath: String? = null
)

@Serializable
data class UserUpdateRequest(
    val userName: String,
    val extraUserInfo: String? = null,
    val profileImagePath: String? = null
)

@Serializable
data class LoginRequest(
    val loginId: String,
    val password: String
)

@Serializable
data class UserPrincipal(
    val id: Long,
    val loginId: String,
    val userName: String,
    val roles: List<String>
)