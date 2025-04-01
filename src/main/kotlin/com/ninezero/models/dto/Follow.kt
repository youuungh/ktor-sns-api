package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Follow(
    val id: Long = 0,
    val followerId: Long,
    val followingId: Long,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null
)
