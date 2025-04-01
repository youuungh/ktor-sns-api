package com.ninezero.plugins

import com.ninezero.exception.*
import com.ninezero.exception.UnsupportedMediaTypeException
import com.ninezero.models.dto.CommonResponse
import com.ninezero.models.error.ErrorCode
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        // 400 Bad Request
        exception<BadRequestException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = CommonResponse.fail("400", cause.message ?: "Bad Request")
            )
        }

        // Parameter Validation Failed
        exception<ParameterConversionException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = CommonResponse.fail("400", "Parameter validation failed: ${cause.message}")
            )
        }

        // 401 Unauthorized - JWT 검증 실패
        exception<AuthenticationException> { call, _ ->
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = CommonResponse.error<Any>(ErrorCode.USER_FAIL_AUTHORIZATION)
            )
        }

        // 403 Forbidden
        exception<ForbiddenException> { call, _ ->
            call.respond(
                status = HttpStatusCode.Forbidden,
                message = CommonResponse.error<Any>(ErrorCode.USER_FAIL_ACCESS)
            )
        }

        // 404 Not Found
        exception<NotFoundException> { call, cause ->
            call.respond(
                status = HttpStatusCode.NotFound,
                message = CommonResponse.fail("404", cause.message ?: "Not Found")
            )
        }

        // 413 Payload Too Large
        exception<RequestTooBigException> { call, _ ->
            call.respond(
                status = HttpStatusCode.PayloadTooLarge,
                message = CommonResponse.fail("413", "File size exceeds maximum limit")
            )
        }

        // 415 Unsupported Media Type
        exception<UnsupportedMediaTypeException> { call, cause ->
            call.respond(
                status = HttpStatusCode.UnsupportedMediaType,
                message = CommonResponse.fail("415", cause.message ?: "Unsupported Media Type")
            )
        }

        // Custom Validation Exception
        exception<ValidationException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = CommonResponse.error<Any>(cause.error, cause.reason)
            )
        }

        // IllegalArgumentException 및 기타 예외
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = CommonResponse.fail("400", cause.message ?: "Bad Request")
            )
        }

        // 500 Internal Server Error
        exception<Throwable> { call, cause ->
            call.application.log.error("Internal Server Error", cause)
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = CommonResponse.fail("500", "Internal Server Error")
            )
        }
    }
}