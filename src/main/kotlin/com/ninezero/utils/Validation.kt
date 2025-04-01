package com.ninezero.utils

import com.ninezero.models.dto.*

object Validation {
    fun validateLoginId(loginId: String): Boolean {
        return loginId.matches(Regex("^[a-zA-Z0-9]{4,20}$"))
    }

    fun validatePassword(password: String): Boolean {
        // 비밀번호 규칙: 최소 12자, 영문 대/소문자, 숫자, 특수문자 중 3가지 이상 조합
        val pattern =
            """^(?:(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])|(?=.*[A-Z])(?=.*[a-z])(?=.*[!@#$%^&*])|(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])|(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*]))[\w!@#$%^&*]{12,}$"""
        return password.matches(Regex(pattern))
    }

    fun validateName(name: String): Boolean {
        return name.length in 2..50
    }

    // NotBlank 검증 추가
    fun validateNotBlank(value: String, fieldName: String) {
        require(value.isNotBlank()) { "${fieldName}은 필수 입력 값 입니다" }
    }

    fun validateBoardRequest(request: BoardRequest) {
        validateNotBlank(request.title, "게시글 제목")
    }

    fun validateCommentRequest(request: CommentRequest) {
        validateNotBlank(request.comment, "댓글 내용")
    }

    fun validateFileRequest(request: FileUploadRequest) {
        validateNotBlank(request.fileName, "파일명")
    }

    fun validateUserCreateRequest(request: UserCreateRequest) {
        validateNotBlank(request.loginId, "로그인 아이디")
        validateNotBlank(request.userName, "사용자 이름")
        validateNotBlank(request.password, "비밀번호")
        require(validateLoginId(request.loginId)) { "아이디 형식이 올바르지 않습니다" }
        //require(validatePassword(request.password)) { "비밀번호 형식이 올바르지 않습니다" }
        require(validateName(request.userName)) { "이름은 2-50자 사이여야 합니다" }
    }

    fun validateLoginRequest(request: LoginRequest) {
        validateNotBlank(request.loginId, "로그인 아이디")
        validateNotBlank(request.password, "비밀번호")
    }

    fun validateUserUpdateRequest(request: UserUpdateRequest) {
        validateNotBlank(request.userName, "사용자 이름")
        require(validateName(request.userName)) { "이름은 2-50자 사이여야 합니다" }
    }

    val allowedContentTypes = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp"
    )
}