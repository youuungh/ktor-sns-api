package com.ninezero.models.dto

import com.ninezero.models.error.ErrorCode
import kotlinx.serialization.Serializable

@Serializable
data class CommonResponse<T>(
    val result: Result = Result.SUCCESS,
    val data: T? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun <T> success(data: T) = CommonResponse(
            result = Result.SUCCESS,
            data = data,
            errorCode = ErrorCode.SUCCESS.errorCode,
            errorMessage = ErrorCode.SUCCESS.errorMessage
        )

        fun <T> error(error: ErrorCode, reason: String? = null) = CommonResponse<Unit>(
            result = Result.FAIL,
            data = null,
            errorCode = error.errorCode,
            errorMessage = error.errorMessage + (reason?.let { ": $it" } ?: "")
        )

        fun fail(statusCode: String, message: String) = CommonResponse<Unit>(
            result = Result.FAIL,
            data = null,
            errorCode = statusCode,
            errorMessage = message
        )
    }

    @Serializable
    enum class Result { SUCCESS, FAIL }
}