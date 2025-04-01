package com.ninezero.services

import com.ninezero.models.dto.FileResponse
import com.ninezero.models.dto.FileUploadRequest
import io.ktor.http.*
import io.ktor.http.content.*

interface FileService {
    //suspend fun upload(request: FileUploadRequest, file: PartData.FileItem, requesterId: Long): FileResponse
    suspend fun upload(request: FileUploadRequest, fileBytes: ByteArray, contentType: ContentType, requesterId: Long): FileResponse
    suspend fun downloadFile(fileId: String): ByteArray
    suspend fun deleteFile(fileId: String)
}