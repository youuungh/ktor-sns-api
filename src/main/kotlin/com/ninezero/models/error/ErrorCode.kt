package com.ninezero.models.error

import kotlinx.serialization.Serializable

@Serializable
enum class ErrorCode(
    val errorCode: String,
    val errorMessage: String
) {
    SUCCESS("GEN_000", "Request Success"),

    // user
    USER_LOGIN_ID_ALREADY_EXISTED("USR_001", "이미 존재하는 계정입니다"),
    USER_FAIL_LOGIN("USR_002", "로그인에 실패하였습니다"),
    NOT_FOUND_USER("USR_003", "조회되지 않는 사용자입니다"),
    ALREADY_FOLLOWING("USR_004", "이미 팔로우하고 있는 사용자입니다"),
    NOT_FOLLOWING("USR_005", "팔로우하고 있지 않은 사용자입니다"),
    INVALID_FOLLOW_REQUEST("USR_006", "자기 자신을 팔로우할 수 없습니다"),

    // 소셜 로그인 관련 에러 코드 추가
    SOCIAL_AUTH_FAILED("USR_007", "소셜 로그인 인증에 실패했습니다"),
    SOCIAL_PROVIDER_NOT_SUPPORTED("USR_008", "지원하지 않는 소셜 로그인 제공자입니다"),
    SOCIAL_USER_INFO_FAILED("USR_009", "소셜 로그인 사용자 정보를 가져오는데 실패했습니다"),
    SOCIAL_TOKEN_INVALID("USR_010", "유효하지 않은 소셜 로그인 토큰입니다"),

    // invalid param
    INVALID_PARAMETER("GEN_100", "유효하지 않은 파라미터입니다"),

    // auth
    USER_FAIL_AUTHORIZATION("AUTH_001", "인증이 필요합니다"),
    USER_FAIL_ACCESS("AUTH_002", "권한이 필요합니다"),

    // board
    NOT_FOUND_BOARD("BRD_001", "해당 게시글은 삭제되었거나 존재하지 않는 게시글입니다"),
    INVALID_ACCESS_TO_BOARD("BRD_002", "작성자 외에 게시물을 수정하거나 삭제할 수 없습니다"),
    ALREADY_LIKED("BRD_003", "이미 좋아요를 눌렀습니다"),
    ALREADY_CANCELED("BRD_004", "이미 취소되었습니다"),
    CANNOT_LIKE_OWN_BOARD("BRD_005", "내 게시글에는 좋아요를 할 수 없습니다"),
    ALREADY_SAVED("BRD_006", "이미 저장된 게시글입니다."),
    ALREADY_UNSAVED("BRD_007", "이미 저장 취소된 게시글입니다."),
    CANNOT_SAVE_OWN_BOARD("BRD_008", "자신의 게시글은 저장할 수 없습니다."),

    // comment
    NOT_FOUND_COMMENT("CMT_001", "해당 댓글은 이미 삭제되었거나 존재하지 않습니다"),
    INVALID_ACCESS_TO_COMMENT("CMT_002", "댓글에 대한 권한이 없습니다."),
    INVALID_REPLY_DEPTH("CMT_003", "대댓글에는 답글을 달 수 없습니다."),

    // file
    FILE_UPLOAD_ERROR("FILE_001", "파일 업로드 중 오류가 발생했습니다."),
    FILE_NOT_FOUND("FILE_002", "파일을 찾을 수 없습니다."),
    INVALID_FILE("FILE_003", "유효하지 않은 파일입니다"),
    FILE_DELETE_ERROR("FILE_004", "파일 삭제 중 오류가 발생했습니다."),
    FILE_DOWNLOAD_ERROR("FILE_005", "파일 다운로드 중 오류가 발생했습니다."),
    FILE_SYSTEM_ERROR("FILE_006", "파일 시스템 오류가 발생했습니다."),
    FILE_SIZE_EXCEEDED("FILE_007", "파일 크기가 제한을 초과했습니다."),

    // chat
    NOT_FOUND_CHAT_ROOM("CHAT_001", "존재하지 않는 채팅방입니다"),
    INVALID_CHAT_ROOM_ACCESS("CHAT_002", "채팅방에 접근 권한이 없습니다"),
    CHAT_CONNECTION_ERROR("CHAT_003", "채팅 연결 중 오류가 발생했습니다"),

    // server error
    INTERNAL_SERVER_ERROR("GEN_999", "서버 내부 오류")
}