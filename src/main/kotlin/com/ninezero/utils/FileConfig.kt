package com.ninezero.utils

object FileConfig {
    private const val BUCKET_NAME = "upload-file"
    private const val MAX_FILE_SIZE = 16777216  // 16MB

    fun createFilePath(fileId: String): String = "$BUCKET_NAME/$fileId"

    fun extractFileId(filePath: String): String? =
        filePath.takeIf { it.isNotBlank() }
            ?.substringAfterLast("/")
            ?.takeIf { it.isNotBlank() }

    fun getMaxFileSize(): Int = MAX_FILE_SIZE
}