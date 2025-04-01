package com.ninezero.routes

import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.*
import com.ninezero.models.error.ErrorCode
import com.ninezero.services.UserService
import com.ninezero.utils.JwtUtil
import com.ninezero.utils.Validation
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*

fun Route.userRoutes(userService: UserService) {
    route("/api/users") {
        // 회원가입
        post("/sign-up") {
            val request = call.receive<UserCreateRequest>()
            Validation.validateUserCreateRequest(request)
            val id = userService.create(request)
            call.respond(CommonResponse.success(id), typeInfo<CommonResponse<Long>>())
        }

        // 로그인
        post("/login") {
            val request = call.receive<LoginRequest>()
            Validation.validateLoginRequest(request)
            val user = userService.authenticate(request)
            val token = JwtUtil.generateToken(user)
            call.respond(CommonResponse.success(token), typeInfo<CommonResponse<String>>())
        }

        // 소셜 로그인
        post("/social-login") {
            val request = call.receive<SocialLoginRequest>()
            val user = userService.authenticateSocial(request)
            val token = JwtUtil.generateToken(user)
            call.respond(CommonResponse.success(token), typeInfo<CommonResponse<String>>())
        }

        authenticate("auth-jwt") {
            // 내 정보 조회
            get("/my-page") {
                val principal = call.principal<UserPrincipal>()!!
                val user = userService.getById(principal.id, principal.id)
                call.respond(CommonResponse.success(user), typeInfo<CommonResponse<UserResponse>>())
            }

            // 정보 수정
            patch("/my-page") {
                val principal = call.principal<UserPrincipal>()!!
                val request = call.receive<UserUpdateRequest>()
                Validation.validateUserUpdateRequest(request)
                val updatedUser = userService.update(principal.id, request)
                //call.respond(CommonResponse.success(updatedUser.id), typeInfo<CommonResponse<UserResponse>>())
                call.respond(CommonResponse.success(updatedUser.id), typeInfo<CommonResponse<Long>>())
            }

            // 전체 사용자 조회
            get {
                val principal = call.principal<UserPrincipal>()!!
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 10
                val users = userService.getAllUsers(page, size, principal.id)
                call.respond(CommonResponse.success(users), typeInfo<CommonResponse<List<UserResponse>>>())
            }

            // 특정 사용자 조회
            get("/{id}") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val user = userService.getById(id, principal.id)
                call.respond(CommonResponse.success(user), typeInfo<CommonResponse<UserResponse>>())
            }

            // 팔로우
            post("/{id}/follow") {
                val principal = call.principal<UserPrincipal>()!!
                val targetId = call.parameters["id"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val followId = userService.follow(principal.id, targetId)
                call.respond(CommonResponse.success(followId), typeInfo<CommonResponse<Long>>())
            }

            // 팔로우 취소
            delete("/{id}/follow") {
                val principal = call.principal<UserPrincipal>()!!
                val targetId = call.parameters["id"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val unfollowId = userService.unfollow(principal.id, targetId)
                call.respond(CommonResponse.success(unfollowId), typeInfo<CommonResponse<Long>>())
            }

            // 사용자 검색
            get("/search") {
                val principal = call.principal<UserPrincipal>()!!
                val query = call.parameters["query"]
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20

                val users = userService.searchUsers(query, page, size, principal.id)
                call.respond(
                    CommonResponse.success(users),
                    typeInfo<CommonResponse<List<UserResponse>>>()
                )
            }

            // 최근 검색 기록 조회
            get("/recent-searches") {
                val principal = call.principal<UserPrincipal>()!!
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 10

                val recentSearches = userService.getRecentSearches(principal.id, limit)
                call.respond(
                    CommonResponse.success(recentSearches),
                    typeInfo<CommonResponse<List<RecentSearch>>>()
                )
            }

            // 최근 검색 기록 저장
            post("/recent-searches/{userId}") {
                val principal = call.principal<UserPrincipal>()!!
                val searchedUserId = call.parameters["userId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)

                userService.saveRecentSearch(principal.id, searchedUserId)
                call.respond(CommonResponse.success(null), typeInfo<CommonResponse<Unit>>())
            }

            // 특정 최근 검색 기록 삭제
            delete("/recent-searches/{userId}") {
                val principal = call.principal<UserPrincipal>()!!
                val searchedUserId = call.parameters["userId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)

                userService.deleteRecentSearch(principal.id, searchedUserId)
                call.respond(CommonResponse.success(null), typeInfo<CommonResponse<Unit>>())
            }

            // 모든 최근 검색 기록 삭제
            delete("/recent-searches") {
                val principal = call.principal<UserPrincipal>()!!
                userService.clearRecentSearches(principal.id)
                call.respond(CommonResponse.success(null), typeInfo<CommonResponse<Unit>>())
            }
        }
    }
}