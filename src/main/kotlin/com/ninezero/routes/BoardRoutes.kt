package com.ninezero.routes

import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.*
import com.ninezero.models.error.ErrorCode
import com.ninezero.services.BoardService
import com.ninezero.utils.Validation
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*

fun Route.boardRoutes(boardService: BoardService) {
    route("/api/boards") {
        authenticate("auth-jwt") {
            // 게시글 작성
            post {
                val principal = call.principal<UserPrincipal>()!!
                val request = call.receive<BoardRequest>()
                Validation.validateBoardRequest(request)
                val id = boardService.create(request, principal.id)
                call.respond(CommonResponse.success(id), typeInfo<CommonResponse<Long>>())
            }

            // 게시글 목록 조회
            get {
                val principal = call.principal<UserPrincipal>()!!
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 10
                val list = boardService.getList(page, size, principal.id)
                call.respond(CommonResponse.success(list), typeInfo<CommonResponse<List<BoardResponse>>>())
            }

            // 내 게시글 목록 조회
            get("/my-boards") {
                val principal = call.principal<UserPrincipal>()!!
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 10
                val list = boardService.getMyList(page, size, principal.id)
                call.respond(CommonResponse.success(list), typeInfo<CommonResponse<List<BoardResponse>>>())
            }

            // 저장된 게시글 목록 조회
            get("/saved-boards") {
                val principal = call.principal<UserPrincipal>()!!
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 10
                val list = boardService.getSavedList(page, size, principal.id)
                call.respond(CommonResponse.success(list), typeInfo<CommonResponse<List<BoardResponse>>>())
            }

            // 특정 사용자 게시글 목록 조회
            get("/user/{userId}") {
                val principal = call.principal<UserPrincipal>()!!
                val userId = call.parameters["userId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 10
                val list = boardService.getListById(userId, page, size, principal.id)
                call.respond(CommonResponse.success(list), typeInfo<CommonResponse<List<BoardResponse>>>())
            }

            // 특정 게시글 수정
            patch("/{id}") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val request = call.receive<BoardRequest>()
                Validation.validateBoardRequest(request)
                val updatedId = boardService.update(id, request, principal.id)
                call.respond(CommonResponse.success(updatedId), typeInfo<CommonResponse<Long>>())
            }

            // 특정 게시글 삭제
            delete("/{id}") {
                val principal = call.principal<UserPrincipal>()!!
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val deletedId = boardService.delete(id, principal.id)
                call.respond(CommonResponse.success(deletedId), typeInfo<CommonResponse<Long>>())
            }

            // 댓글 목록 조회
            get("/{boardId}/comments") {
                val boardId = call.parameters["boardId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 20

                val comments = boardService.getComments(
                    boardId = boardId,
                    page = page,
                    size = size
                )

                call.respond(
                    CommonResponse.success(comments),
                    typeInfo<CommonResponse<List<CommentResponse>>>()
                )
            }

            // 대댓글 목록 조회
            get("/{boardId}/comments/{parentId}/replies") {
                val boardId = call.parameters["boardId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val parentId = call.parameters["parentId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)

                val replies = boardService.getReplies(boardId, parentId)
                call.respond(
                    CommonResponse.success(replies),
                    typeInfo<CommonResponse<List<CommentResponse>>>()
                )
            }

            // 댓글 작성
            post("/{boardId}/comments") {
                val principal = call.principal<UserPrincipal>()!!
                val boardId = call.parameters["boardId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val request = call.receive<CommentRequest>()
                Validation.validateCommentRequest(request)
                val commentId = boardService.createComment(boardId, request, principal.id)
                call.respond(CommonResponse.success(commentId), typeInfo<CommonResponse<Long>>())
            }

            // 댓글 삭제
            delete("/{boardId}/comments/{commentId}") {
                val principal = call.principal<UserPrincipal>()!!
                val boardId = call.parameters["boardId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val commentId = call.parameters["commentId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val deletedId = boardService.deleteComment(boardId, commentId, principal.id)
                call.respond(CommonResponse.success(deletedId), typeInfo<CommonResponse<Long>>())
            }

            // 게시글 좋아요
            post("/{boardId}/like") {
                val principal = call.principal<UserPrincipal>()!!
                val boardId = call.parameters["boardId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val likeId = boardService.like(boardId, principal.id)
                call.respond(CommonResponse.success(likeId), typeInfo<CommonResponse<Long>>())
            }

            // 게시글 좋아요 취소
            delete("/{boardId}/like") {
                val principal = call.principal<UserPrincipal>()!!
                val boardId = call.parameters["boardId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val unlikeId = boardService.unlike(boardId, principal.id)
                call.respond(CommonResponse.success(unlikeId), typeInfo<CommonResponse<Long>>())
            }

            // 게시글 저장
            post("/{boardId}/save") {
                val principal = call.principal<UserPrincipal>()!!
                val boardId = call.parameters["boardId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val savedId = boardService.saveBoard(boardId, principal.id)
                call.respond(CommonResponse.success(savedId), typeInfo<CommonResponse<Long>>())
            }

            // 게시글 저장 취소
            delete("/{boardId}/save") {
                val principal = call.principal<UserPrincipal>()!!
                val boardId = call.parameters["boardId"]?.toLongOrNull()
                    ?: throw ValidationException(ErrorCode.INVALID_PARAMETER)
                val unsavedId = boardService.unsaveBoard(boardId, principal.id)
                call.respond(CommonResponse.success(unsavedId), typeInfo<CommonResponse<Long>>())
            }
        }
    }
}