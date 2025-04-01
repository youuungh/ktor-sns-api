package com.ninezero.services.social

import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.SocialUserInfo
import com.ninezero.models.error.ErrorCode
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

class KakaoLoginProvider(
    private val client: HttpClient
) : SocialLoginProvider {
    private val logger = LoggerFactory.getLogger(this::class.java)
    override val providerType: String = "kakao"

    override suspend fun verifyToken(token: String): SocialUserInfo {
        try {
            // 카카오 API 호출하여 사용자 정보 가져오기
            val response = client.get("https://kapi.kakao.com/v2/user/me") {
                headers {
                    append("Authorization", "Bearer $token")
                    append("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                }
            }
            val responseBody = response.bodyAsText()
            val jsonObject = Json.parseToJsonElement(responseBody).jsonObject
            val id = jsonObject["id"]?.jsonPrimitive?.content
                ?: throw ValidationException(ErrorCode.SOCIAL_USER_INFO_FAILED, "카카오 ID 없음")

            val properties = jsonObject["properties"]?.jsonObject
            val kakaoAccount = jsonObject["kakao_account"]?.jsonObject
            val nickname = properties?.get("nickname")?.jsonPrimitive?.content
                ?: kakaoAccount?.get("profile")?.jsonObject?.get("nickname")?.jsonPrimitive?.content
            val profileImage = properties?.get("profile_image")?.jsonPrimitive?.content
                ?: kakaoAccount?.get("profile")?.jsonObject?.get("profile_image_url")?.jsonPrimitive?.content
            val email = kakaoAccount?.get("email")?.jsonPrimitive?.content

            val emailId = email?.substringBefore('@') ?: nickname ?: id
            val userName = nickname ?: email?.substringBefore('@') ?: "Kakao User"

            return SocialUserInfo(
                id = id,
                emailId = emailId,
                userName = userName,
                profileImage = profileImage,
                provider = providerType
            )
        } catch (e: Exception) {
            logger.error("카카오 인증 실패: ${e.message}", e)
            if (e is ValidationException) throw e
            throw ValidationException(ErrorCode.SOCIAL_AUTH_FAILED, "카카오 인증 실패: ${e.message}")
        }
    }
}