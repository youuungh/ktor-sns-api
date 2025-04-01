package com.ninezero.plugins

import com.ninezero.models.dto.ChatSession
import com.ninezero.models.dto.CommonResponse
import com.ninezero.models.dto.UserPrincipal
import com.ninezero.models.error.ErrorCode
import com.ninezero.utils.JwtUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.util.*

fun Application.configureSecurity() {
    JwtUtil.initialize(this)

    install(Sessions) {
        cookie<ChatSession>("CHAT_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600
        }
    }

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JwtUtil.getVerifier())
            @Suppress("UNCHECKED_CAST")
            validate { credential ->
                try {
                    val info = credential.payload.getClaim("info")
                    if (info != null) {
                        UserPrincipal(
                            id = info.asMap()["id"].toString().toLong(),
                            loginId = info.asMap()["loginId"].toString(),
                            userName = info.asMap()["userName"].toString(),
                            roles = info.asMap()["roles"] as List<String>
                        )
                    } else null
                } catch (e: Exception) {
                    println("JWT validation failed: ${e.message}")
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    CommonResponse.error<Any>(ErrorCode.USER_FAIL_AUTHORIZATION)
                )
            }
        }
    }
}

fun generateNonce() = UUID.randomUUID().toString()