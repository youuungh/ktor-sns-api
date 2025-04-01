package com.ninezero.plugins

import com.ninezero.routes.*
import com.ninezero.services.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val userService by inject<UserService>()
    val boardService by inject<BoardService>()
    val fileService by inject<FileService>()
    val chatService by inject<ChatService>()
    val notificationService by inject<NotificationService>()

    routing {
        get("/") {
            call.respondRedirect("/api-docs")
        }

        userRoutes(userService)
        boardRoutes(boardService)
        fileRoutes(fileService)
        chatRoutes(chatService)
        notificationRoutes(notificationService)
    }
}