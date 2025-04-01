package com.ninezero.services

import com.ninezero.models.dto.*
import io.ktor.websocket.*

interface ChatService {
    suspend fun createRoom(request: CreateChatRoomRequest, creatorId: Long): String
    suspend fun checkExistingRoom(userId: Long, otherUserId: Long): String?
    suspend fun getRooms(userId: Long, page: Int, size: Int): List<ChatRoomResponse>
    suspend fun getMessages(userId: Long, roomId: String, page: Int, size: Int): List<ChatMessage>
    suspend fun connect(session: WebSocketSession, chatSession: ChatSession)
    suspend fun disconnect(userId: Long)
    suspend fun sendMessage(chatSession: ChatSession, message: ChatMessageRequest)
    suspend fun markAsRead(userId: Long, roomId: String, messageId: String)
    suspend fun leaveRoom(userId: Long, roomId: String)
}