package com.ninezero.services

import com.ninezero.models.dto.FCMNotification

interface FCMService {
    fun initialize()
    suspend fun sendNotification(token: String, notification: FCMNotification)
    suspend fun sendMulticast(tokens: List<String>, notification: FCMNotification)
}