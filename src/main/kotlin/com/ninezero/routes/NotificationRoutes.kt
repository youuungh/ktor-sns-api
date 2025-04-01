package com.ninezero.routes

import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.CommonResponse
import com.ninezero.models.dto.DeviceTokenRequest
import com.ninezero.models.dto.NotificationResponse
import com.ninezero.models.dto.UserPrincipal
import com.ninezero.models.error.ErrorCode
import com.ninezero.services.NotificationService
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*

fun Route.notificationRoutes(notificationService: NotificationService) {
    route("/api/notifications") {
        authenticate("auth-jwt") {
            // 푸쉬 알림
            post("/tokens") {
                val principal = call.principal<UserPrincipal>()!!
                val request = call.receive<DeviceTokenRequest>()
                val id = notificationService.registerDeviceToken(principal.id, request)
                call.respond(CommonResponse.success(id), typeInfo<CommonResponse<Long>>())
            }

            // 디바이스 토큰 삭제
            delete("/tokens/{token}") {
                val principal = call.principal<UserPrincipal>()!!
                val token = call.parameters["token"]
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)

                val success = notificationService.removeDeviceToken(principal.id, token)
                call.respond(CommonResponse.success(success), typeInfo<CommonResponse<Boolean>>())
            }

            // 알림 목록 조회
            get {
                val principal = call.principal<UserPrincipal>()!!
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20
                val notifications = notificationService.getNotifications(principal.id, page, size)
                call.respond(CommonResponse.success(notifications), typeInfo<CommonResponse<List<NotificationResponse>>>())
            }

            // 읽지 않은 알림 확인
            get("/unread") {
                val principal = call.principal<UserPrincipal>()!!
                val hasUnread = notificationService.hasUnreadNotifications(principal.id)
                call.respond(CommonResponse.success(hasUnread), typeInfo<CommonResponse<Boolean>>())
            }

            // 알림 읽음 처리
            post("/{id}/read") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val success = notificationService.markAsRead(principal.id, id)
                call.respond(CommonResponse.success(success), typeInfo<CommonResponse<Boolean>>())
            }

            // 알림 삭제
            delete("/{id}") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]?.toLongOrNull() ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val success = notificationService.deleteNotification(principal.id, id)
                call.respond(CommonResponse.success(success), typeInfo<CommonResponse<Boolean>>())
            }

            // 알림 전체 삭제
            delete("/all") {
                val principal = call.principal<UserPrincipal>()!!
                val success = notificationService.deleteAllNotifications(principal.id)
                call.respond(CommonResponse.success(success), typeInfo<CommonResponse<Boolean>>())
            }
        }
    }
}