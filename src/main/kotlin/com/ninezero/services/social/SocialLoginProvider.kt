package com.ninezero.services.social

import com.ninezero.models.dto.SocialUserInfo

interface SocialLoginProvider {
    val providerType: String
    suspend fun verifyToken(token: String): SocialUserInfo
}