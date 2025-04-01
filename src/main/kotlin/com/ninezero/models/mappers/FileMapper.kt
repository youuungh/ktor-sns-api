package com.ninezero.models.mappers

import com.ninezero.models.dto.FileResponse
import com.ninezero.models.tables.Files
import org.jetbrains.exposed.sql.ResultRow

object FileMapper {
    fun toFileResponse(row: ResultRow) = FileResponse(
        id = row[Files.id].value,
        fileName = row[Files.fileName],
        createdAt = row[Files.createdAt],
        filePath = row[Files.filePath],
    )
}