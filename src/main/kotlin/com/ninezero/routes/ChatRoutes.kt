package com.ninezero.routes

import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.*
import com.ninezero.models.error.ErrorCode
import com.ninezero.plugins.generateNonce
import com.ninezero.services.ChatService
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.reflect.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun Route.chatRoutes(chatService: ChatService) {
    val logger = LoggerFactory.getLogger("ChatRoutes")

    route("/api/chat") {
        authenticate("auth-jwt") {
            // 채팅방 생성
            post("/rooms") {
                val principal = call.principal<UserPrincipal>()!!
                val request = call.receive<CreateChatRoomRequest>()
                val roomId = chatService.createRoom(request, principal.id)
                call.respond(CommonResponse.success(roomId), typeInfo<CommonResponse<String>>())
            }

            // 기존 채팅방 유무 확인
            get("/rooms/check/{userId}") {
                val principal = call.principal<UserPrincipal>()!!
                val otherUserId = call.parameters["userId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)

                val roomId = chatService.checkExistingRoom(principal.id, otherUserId)
                call.respond(CommonResponse.success(roomId), typeInfo<CommonResponse<String?>>())
            }

            // 내 채팅방 목록 조회
            get("/rooms") {
                val principal = call.principal<UserPrincipal>()!!
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20
                val rooms = chatService.getRooms(principal.id, page, size)
                call.respond(CommonResponse.success(rooms), typeInfo<CommonResponse<List<ChatRoomResponse>>>())
            }

            // 특정 채팅방의 메시지 이력 조회
            get("/rooms/{roomId}/messages") {
                val principal = call.principal<UserPrincipal>()!!
                val roomId = call.parameters["roomId"] ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20
                val messages = chatService.getMessages(principal.id, roomId, page, size)
                call.respond(CommonResponse.success(messages), typeInfo<CommonResponse<List<ChatMessage>>>())
            }

            // 메시지 읽음 처리
            post("/rooms/{roomId}/messages/{messageId}/read") {
                val principal = call.principal<UserPrincipal>()!!
                val roomId = call.parameters["roomId"]
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val messageId = call.parameters["messageId"]
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)

                chatService.markAsRead(principal.id, roomId, messageId)
                call.respond(CommonResponse.success(null), typeInfo<CommonResponse<Unit>>())
            }

            // 채팅방 나가기 API
            delete("/rooms/{roomId}") {
                val principal = call.principal<UserPrincipal>()!!
                val roomId = call.parameters["roomId"]
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)

                chatService.leaveRoom(principal.id, roomId)
                call.respond(CommonResponse.success(null), typeInfo<CommonResponse<Unit>>())
            }

            // WebSocket 채팅 연결
            webSocket("/ws") {
                val principal = call.principal<UserPrincipal>()!!
                val session = ChatSession(
                    userId = principal.id,
                    userName = principal.userName,
                    sessionId = generateNonce()
                )

//            // 테스트용 세션
//            val session = ChatSession(
//                userId = 1L,
//                userName = "TestUser",
//                sessionId = generateNonce()
//            )

                try {
                    // WebSocket 연결 설정
                    chatService.connect(this, session)

                    // 메시지 수신 및 처리
                    incoming.consumeEach { frame ->
                        try {
                            when (frame) {
                                is Frame.Text -> {
                                    val messageText = frame.readText()

                                    try {
                                        val sessionInfo = Json.decodeFromString<ChatSession>(messageText)
                                        logger.debug("Processed session info: {}", sessionInfo)
                                        return@consumeEach
                                    } catch (e: Exception) {
                                        try {
                                            val request = Json.decodeFromString<ChatMessageRequest>(messageText)
                                            logger.debug("Processing chat message request: {}", request)
                                            chatService.sendMessage(session, request)
                                        } catch (e: Exception) {
                                            logger.error("Error processing message: ${e.message}", e)
                                            send(Frame.Text(Json.encodeToString(
                                                CommonResponse.error<String>(ErrorCode.INTERNAL_SERVER_ERROR)
                                            )))
                                        }
                                    }
                                }
                                else -> {
                                    logger.debug("Unsupported frame type: {}", frame)
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("Error processing message: ${e.message}", e)
                            send(Frame.Text(Json.encodeToString(
                                CommonResponse.error<String>(ErrorCode.INTERNAL_SERVER_ERROR)
                            )))
                        }
                    }
                } catch (e: ValidationException) {
                    send(Frame.Text(Json.encodeToString(
                        CommonResponse.error<String>(e.error, e.reason)
                    )))
                } catch (e: Exception) {
                    logger.error("WebSocket error: ${e.message}", e)
                    send(Frame.Text(Json.encodeToString(
                        CommonResponse.error<String>(ErrorCode.INTERNAL_SERVER_ERROR)
                    )))
                } finally {
                    chatService.disconnect(session.userId)
                }
            }
        }
    }
}