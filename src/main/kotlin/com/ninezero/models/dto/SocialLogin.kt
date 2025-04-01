package com.ninezero.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginRequest(
    val token: String,
    val provider: String
)

data class SocialUserInfo(
    val id: String,
    val emailId: String?,
    val userName: String,
    val profileImage: String?,
    val provider: String
)