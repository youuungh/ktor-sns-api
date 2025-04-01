package com.ninezero.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.ninezero.models.dto.UserResponse
import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import java.util.Date

object JwtUtil {
    private lateinit var secret: String
    private lateinit var issuer: String
    private lateinit var audience: String
    private var expirationTime: Long = 3600000L
    private lateinit var algorithm: Algorithm
    private lateinit var verifier: JWTVerifier

    fun initialize(application: Application) {
        secret = application.environment.config.property("jwt.secret-key").getString()
        issuer = application.environment.config.property("jwt.issuer").getString()
        audience = application.environment.config.property("jwt.audience").getString()
        expirationTime = application.environment.config.property("jwt.expired-time")
            .getString().toLong()

        algorithm = Algorithm.HMAC256(secret)
        verifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }

    fun generateToken(user: UserResponse): String {
        val now = Date()
        val exp = Date(now.time + expirationTime)

        val tokenInfo = TokenInfo(
            id = user.id,
            loginId = user.loginId,
            userName = user.userName,
            roles = listOf("user")
        )

        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("info", mapOf(
                "id" to tokenInfo.id,
                "loginId" to tokenInfo.loginId,
                "userName" to tokenInfo.userName,
                "roles" to tokenInfo.roles
            ))
            .withIssuedAt(now)
            .withExpiresAt(exp)
            .sign(algorithm)
    }

    fun getVerifier(): JWTVerifier = verifier
}

@Serializable
data class TokenInfo(
    val id: Long,
    val loginId: String,
    val userName: String,
    val roles: List<String>
)