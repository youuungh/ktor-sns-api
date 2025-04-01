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

class NaverLoginProvider(
    private val client: HttpClient
) : SocialLoginProvider {
    private val logger = LoggerFactory.getLogger(this::class.java)
    override val providerType: String = "naver"

    override suspend fun verifyToken(token: String): SocialUserInfo {
        try {
            val response = client.get("https://openapi.naver.com/v1/nid/me") {
                headers {
                    append("Authorization", "Bearer $token")
                }
            }
            val responseBody = response.bodyAsText()
            val jsonObject = Json.parseToJsonElement(responseBody).jsonObject

            val resultCode = jsonObject["resultcode"]?.jsonPrimitive?.content
            if (resultCode != "00") {
                throw ValidationException(ErrorCode.SOCIAL_AUTH_FAILED, "네이버 인증 실패")
            }

            val responseData = jsonObject["response"]?.jsonObject
                ?: throw ValidationException(ErrorCode.SOCIAL_USER_INFO_FAILED, "네이버 응답 데이터 없음")

            val id = responseData["id"]?.jsonPrimitive?.content
                ?: throw ValidationException(ErrorCode.SOCIAL_USER_INFO_FAILED, "네이버 ID 없음")

            val nickname = responseData["nickname"]?.jsonPrimitive?.content
            val name = responseData["name"]?.jsonPrimitive?.content
            val email = responseData["email"]?.jsonPrimitive?.content
            val profileImage = responseData["profile_image"]?.jsonPrimitive?.content

            val emailId = nickname ?: email?.substringBefore('@') ?: name
            val userName = name ?: nickname ?: "Naver User"

            return SocialUserInfo(
                id = id,
                emailId = emailId,
                userName = userName,
                profileImage = profileImage,
                provider = providerType
            )
        } catch (e: Exception) {
            logger.error("네이버 인증 실패: ${e.message}", e)
            if (e is ValidationException) throw e
            throw ValidationException(ErrorCode.SOCIAL_AUTH_FAILED, "네이버 인증 실패: ${e.message}")
        }
    }
}