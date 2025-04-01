package com.ninezero.routes

import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.CommonResponse
import com.ninezero.models.dto.FileResponse
import com.ninezero.models.dto.FileUploadRequest
import com.ninezero.models.dto.UserPrincipal
import com.ninezero.models.error.ErrorCode
import com.ninezero.services.FileService
import com.ninezero.utils.Validation
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import org.slf4j.LoggerFactory

fun Route.fileRoutes(fileService: FileService) {
    val logger = LoggerFactory.getLogger("FileRoutes")

    route("/api/files") {
        authenticate("auth-jwt") {
            // GridFS 업로드
            post {
                try {
                    val principal = call.principal<UserPrincipal>()!!
                    val multipart = call.receiveMultipart()
                    var fileName = ""
                    var fileBytes: ByteArray? = null
                    var fileContentType: ContentType? = null
                    //var fileItem: PartData.FileItem? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                logger.debug("FormItem name: ${part.name}, value: ${part.value}")
                                if (part.name == "fileName") {
                                    fileName = part.value
                                }
                            }
                            is PartData.FileItem -> {
                                logger.debug("FileItem: name=${part.name}, originalFileName=${part.originalFileName}")
                                fileContentType = part.contentType
                                fileBytes = part.provider().readRemaining().readByteArray()
                            }
                            else -> {}
                        }
                        part.dispose()
                    }

                    if (fileName.isBlank() || fileBytes == null) {
                        throw ValidationException(ErrorCode.INVALID_PARAMETER)
                    }

                    val request = FileUploadRequest(fileName)
                    Validation.validateFileRequest(request)
                    val response = fileService.upload(request, fileBytes!!, fileContentType!!, principal.id)
                    call.respond(CommonResponse.success(response), typeInfo<CommonResponse<FileResponse>>())
                } catch (e: Exception) {
                    logger.error("File upload error", e)
                    when (e) {
                        is ValidationException -> throw e
                        is ContentTransformationException -> throw ValidationException(ErrorCode.INVALID_FILE)
                        else -> throw ValidationException(ErrorCode.FILE_SYSTEM_ERROR)
                    }
                }
            }

            // GridFS 삭제
            delete("/{id}") {
                try {
                    val fileId = call.parameters["id"] ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                    fileService.deleteFile(fileId)
                    call.respond(CommonResponse.success(true))
                } catch (e: Exception) {
                    logger.error("File delete error", e)
                    when (e) {
                        is ValidationException -> throw e
                        else -> throw ValidationException(ErrorCode.FILE_SYSTEM_ERROR)
                    }
                }
            }
        }
    }

    // GridFS 다운로드
    get("/upload-file/{id}") {
        try {
            val fileId = call.parameters["id"] ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
            val fileData = fileService.downloadFile(fileId)
            call.respondBytes(fileData)
        } catch (e: Exception) {
            logger.error("File download error", e)
            when (e) {
                is ValidationException -> throw e
                else -> throw ValidationException(ErrorCode.FILE_SYSTEM_ERROR)
            }
        }
    }
}