package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class LikeBoard(
    val id: Long = 0,
    val createdBy: User,
    val board: Board,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null
)