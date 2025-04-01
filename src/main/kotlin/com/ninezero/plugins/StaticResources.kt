package com.ninezero.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureStaticResources() {
    routing {
        if (environment.config.property("h2.console.enabled").getString().toBoolean()) {
            val consolePath = environment.config.property("h2.console.path").getString()
            staticResources(consolePath, "h2-console")
        }
    }
}

/**
fun Application.configureStaticResources() {
    routing {
        val uploadPath = environment.config.property("file.uploadPath").getString()
        val currentDir = File("").absolutePath

        // 정적 파일 서빙
        staticFiles("/upload-file", File("$currentDir/$uploadPath"))

        if (environment.config.property("h2.console.enabled").getString().toBoolean()) {
            val consolePath = environment.config.property("h2.console.path").getString()
            staticResources(consolePath, "h2-console")
        }
    }
}
*/