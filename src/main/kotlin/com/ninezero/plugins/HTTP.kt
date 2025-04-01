package com.ninezero.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureHTTP() {
    routing {
        swaggerUI(path = "api-docs", swaggerFile = "openapi/documentation.yaml") {
            version = "4.15.5"
        }
        openAPI(path = "api-spec", swaggerFile = "openapi/documentation.yaml")
    }
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
        header("X-Frame-Options", "DENY")
        header("X-Content-Type-Options", "nosniff")
        header("X-XSS-Protection", "1; mode=block")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header(
            "Content-Security-Policy",
            "default-src 'self';" +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://unpkg.com;" +
                    "style-src 'self' 'unsafe-inline' https://unpkg.com;" +
                    "img-src 'self' data: https:;" +
                    "connect-src 'self' *;"
        )
    }
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("Token")

        exposeHeader(HttpHeaders.ContentType)
        exposeHeader(HttpHeaders.Authorization)
        exposeHeader("Token")

        allowCredentials = true
        anyHost()
        // allowHost("localhost:8080", "127.0.0.1:8080")

        maxAgeInSeconds = 86400
    }
}
