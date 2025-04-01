package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class RecentSearch(
    val id: Long = 0,
    val userId: Long,
    val searchedUserId: Long,
    val searchedUserName: String,
    val searchedUserProfileImagePath: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val searchedAt: LocalDateTime
)