package com.ninezero.services

import com.mongodb.client.gridfs.model.GridFSUploadOptions
import com.mongodb.reactivestreams.client.gridfs.GridFSBucket
import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.FileResponse
import com.ninezero.models.dto.FileUploadRequest
import com.ninezero.models.error.ErrorCode
import com.ninezero.models.mappers.FileMapper
import com.ninezero.models.mappers.UserMapper
import com.ninezero.models.tables.Files
import com.ninezero.models.tables.Users
import com.ninezero.utils.FileConfig
import com.ninezero.utils.Validation
import io.ktor.http.*
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.Document
import org.bson.types.ObjectId
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.*

class FileServiceImpl(
    private val bucket: GridFSBucket,
) : FileService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // DB 트랜잭션 처리
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(IO) { block() }

    // 파일 업로드 처리
    override suspend fun upload(request: FileUploadRequest, fileBytes: ByteArray, contentType: ContentType, requesterId: Long): FileResponse = dbQuery {
        if (fileBytes.isEmpty()) {
            logger.error("File data is empty.")
            throw ValidationException(ErrorCode.FILE_UPLOAD_ERROR)
        }

        // 파일 유효성 검사
        validateFile(contentType, fileBytes.size)

        val fileName = request.fileName.substringBeforeLast(".", request.fileName) + when {
            contentType.toString().contains("image/jpeg") -> ".jpg"
            contentType.toString().contains("image/png") -> ".png"
            contentType.toString().contains("image/gif") -> ".gif"
            contentType.toString().contains("image/webp") -> ".webp"
            else -> throw ValidationException(ErrorCode.INVALID_FILE)
        }

        Users.select(Users.columns)
            .where { Users.id eq requesterId }
            .map { UserMapper.toUser(it) }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)

        val metadata = Document()
            .append("contentType", contentType.toString())
            .append("userId", requesterId)
            .append("originalFileName", fileName)
            .append("fileSize", fileBytes.size)
            .append("createdAt", LocalDateTime.now().toString())

        var gridFSId: ObjectId? = null

        try {
            val uploadOptions = GridFSUploadOptions().metadata(metadata)
            val byteBuffer = ByteBuffer.wrap(fileBytes)
            gridFSId = bucket.uploadFromPublisher(
                fileName,
                Flux.just(byteBuffer),
                uploadOptions
            ).awaitFirst()

            val filePath = FileConfig.createFilePath(gridFSId.toString())
            val dbFileId = Files.insert {
                it[Files.fileName] = fileName
                it[Files.filePath] = filePath
                it[Files.createUserId] = requesterId
                it[Files.createdAt] = LocalDateTime.now()
            }[Files.id].value

            Files.select(Files.columns)
                .where { Files.id eq dbFileId }
                .map { FileMapper.toFileResponse(it) }
                .first()
        } catch (e: Exception) {
            logger.error("Error during file upload: ${e.message}", e)
            gridFSId?.let {
                try {
                    bucket.delete(it).awaitFirstOrNull()
                } catch (deleteError: Exception) {
                    logger.error("Failed to delete file after upload error: ${deleteError.message}")
                }
            }
            throw ValidationException(ErrorCode.FILE_SYSTEM_ERROR)
        }
    }

    private fun validateFile(contentType: ContentType, fileSize: Int) {
        logger.debug("Validating file. ContentType: {}, Size: {}", contentType, fileSize)

        if (!Validation.allowedContentTypes.contains(contentType.toString())) {
            throw ValidationException(ErrorCode.INVALID_FILE)
        }

        if (fileSize > FileConfig.getMaxFileSize()) {
            logger.warn("File size exceeded. Current: $fileSize, Max: ${FileConfig.getMaxFileSize()}")
            throw ValidationException(ErrorCode.FILE_SIZE_EXCEEDED)
        }
    }

    override suspend fun deleteFile(fileId: String) {
        try {
            bucket.delete(ObjectId(fileId)).awaitFirstOrNull()
        } catch (e: Exception) {
            logger.error("Failed to delete file from GridFS: ${e.message}", e)
            throw ValidationException(ErrorCode.FILE_DELETE_ERROR)
        }
    }

    override suspend fun downloadFile(fileId: String): ByteArray {
        try {
            return Flux.from(bucket.downloadToPublisher(ObjectId(fileId)))
                .collectList()
                .map { buffers ->
                    buffers.fold(ByteArray(0)) { acc, buffer ->
                        acc + buffer.array()
                    }
                }
                .awaitFirst()
        } catch (e: Exception) {
            logger.error("Failed to download file from GridFS: ${e.message}", e)
            throw ValidationException(ErrorCode.FILE_DOWNLOAD_ERROR)
        }
    }
}

/**
class FileServiceImpl(
    private val config: ApplicationConfig
) : FileService {
    private val uploadPath = config.property("file.uploadPath").getString()

    // DB 트랜잭션 처리
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(IO) { block() }

    // 파일 업로드 처리
    override suspend fun upload(request: FileUploadRequest, fileBytes: ByteArray, contentType: ContentType, requesterId: Long): FileResponse = dbQuery {
        println("FileBytes size: ${fileBytes.size}")

        if (fileBytes.isEmpty()) {
            println("File data is empty.")
            throw ValidationException(ErrorCode.FILE_UPLOAD_ERROR)
        }

        validateFile(contentType)

        val extension = when {
            contentType.toString().contains("image/jpeg") -> ".jpg"
            contentType.toString().contains("image/png") -> ".png"
            contentType.toString().contains("image/gif") -> ".gif"
            contentType.toString().contains("image/webp") -> ".webp"
            else -> throw ValidationException(ErrorCode.INVALID_FILE)
        }

        val user = Users.select(Users.columns)
            .where { Users.id eq requesterId }
            .map { UserMapper.toUser(it) }
            .firstOrNull() ?: throw ValidationException(ErrorCode.NOT_FOUND_USER)

        val relativePath = "$uploadPath/${UUID.randomUUID()}$extension"
        val savePath = File(Paths.get("").toAbsolutePath().toString(), relativePath).absolutePath

        val targetFile = File(savePath).apply {
            if (!exists()) parentFile.mkdirs()
        }
        targetFile.writeBytes(fileBytes)

        println("Saved file path: $savePath, size: ${targetFile.length()}")

        val fileId = Files.insert {
            it[fileName] = "${request.fileName}$extension"
            it[filePath] = relativePath
            it[createUserId] = requesterId
            it[createdAt] = LocalDateTime.now()
        }[Files.id].value

        Files.select(Files.columns)
            .where { Files.id eq fileId }
            .map { FileMapper.toFileResponse(it) }
            .first()
    }

    // 파일 유효성 검사
    private fun validateFile(contentType: ContentType) {
        println("File ContentType: $contentType")
        println("File ContentType toString: $contentType")

        if (!Validation.allowedContentTypes.contains(contentType.toString())) {
            throw ValidationException(ErrorCode.INVALID_FILE)
        }
    }
}
*/