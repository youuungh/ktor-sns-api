package com.ninezero.services.social

import com.google.firebase.auth.FirebaseAuth
import com.ninezero.exception.ValidationException
import com.ninezero.models.dto.SocialUserInfo
import com.ninezero.models.error.ErrorCode

class GoogleLoginProvider : SocialLoginProvider {
    override val providerType: String = "google"

    override suspend fun verifyToken(token: String): SocialUserInfo {
        try {
            val firebaseToken = FirebaseAuth.getInstance().verifyIdToken(token)
            val emailId = firebaseToken.email?.substringBefore('@') ?: firebaseToken.name

            return SocialUserInfo(
                id = firebaseToken.uid,
                emailId = emailId,
                userName = firebaseToken.name,
                profileImage = firebaseToken.picture,
                provider = providerType
            )
        } catch (e: Exception) {
            throw ValidationException(ErrorCode.SOCIAL_TOKEN_INVALID, "Google 인증 실패: ${e.message}")
        }
    }
}