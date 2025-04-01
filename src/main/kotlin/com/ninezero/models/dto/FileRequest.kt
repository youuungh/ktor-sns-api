package com.ninezero.models.dto

import com.ninezero.utils.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class FileUploadRequest(
    val fileName: String
)

@Serializable
data class FileResponse(
    val id: Long,
    val fileName: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val filePath: String
)