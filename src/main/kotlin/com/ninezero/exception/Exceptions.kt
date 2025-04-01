package com.ninezero.exception

import com.ninezero.models.error.ErrorCode

class ValidationException(
    val error: ErrorCode,
    val reason: String? = null
) : RuntimeException(error.errorMessage)
class RequestTooBigException(message: String) : RuntimeException(message)
class ForbiddenException(message: String) : RuntimeException(message)
class UnsupportedMediaTypeException(message: String) : RuntimeException(message)
class AuthenticationException(message: String) : RuntimeException(message)