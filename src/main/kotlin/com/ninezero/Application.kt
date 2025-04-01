package com.ninezero

import com.ninezero.plugins.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    validateConfig(environment.config)

    configureSockets()
    configureSerialization()
    configureDatabases()
    configureFrameworks()
    configureMonitoring()
    configureStaticResources()
    configureHTTP()
    configureSecurity()
    configureStatusPages()
    configureRouting()

    // 서버 시작 로그
    logger.info("Server started on port: ${environment.config.property("ktor.deployment.port").getString()}")
}

private fun validateConfig(config: ApplicationConfig) {
    // JWT 설정 검증
    requireNotNull(config.propertyOrNull("jwt.secret-key")?.getString()) { "JWT secret key is required" }
    requireNotNull(config.propertyOrNull("jwt.issuer")?.getString()) { "JWT issuer is required" }
    requireNotNull(config.propertyOrNull("jwt.audience")?.getString()) { "JWT audience is required" }
    requireNotNull(config.propertyOrNull("jwt.expired-time")?.getString()) { "JWT expiration time is required" }

    // 데이터베이스 설정 검증
    requireNotNull(config.propertyOrNull("database.url")?.getString()) { "Database URL is required" }
    requireNotNull(config.propertyOrNull("database.driverClassName")?.getString()) { "Database driver is required" }

    // MongoDB 설정 검증
    requireNotNull(config.propertyOrNull("db.mongo.uri")?.getString()) { "MongoDB URI is required" }
    requireNotNull(config.propertyOrNull("db.mongo.database.name")?.getString()) { "MongoDB database name is required" }
}

/**
// 파일 업로드 설정 검증
requireNotNull(config.propertyOrNull("file.uploadPath")?.getString()) { "File upload path is required" }
requireNotNull(config.propertyOrNull("file.maxFileSize")?.getString()) { "Max file size is required" }
*/