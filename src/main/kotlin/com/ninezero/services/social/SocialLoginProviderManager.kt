package com.ninezero.services.social

import com.ninezero.exception.ValidationException
import com.ninezero.models.error.ErrorCode

class SocialLoginProviderManager(
    private val providers: List<SocialLoginProvider>
) {
    private val providerMap = providers.associateBy { it.providerType }

    fun getProvider(providerType: String): SocialLoginProvider {
        return providerMap[providerType] ?: throw ValidationException(
            ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED,
            "지원하지 않는 소셜 로그인 제공자: $providerType"
        )
    }
}