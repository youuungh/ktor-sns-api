package com.ninezero.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import com.ninezero.models.dto.FCMNotification
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.FileInputStream

class FCMServiceImpl(
    private val config: ApplicationConfig
) : FCMService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun initialize() {
        try {
            val serviceAccountPath = config.property("firebase.credentials.path").getString()
            val serviceAccount = FileInputStream(serviceAccountPath)

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                logger.info("Firebase has been initialized")
            }
        } catch (e: Exception) {
            logger.error("Error initializing Firebase: ${e.message}", e)
            throw e
        }
    }

    override suspend fun sendNotification(token: String, notification: FCMNotification) {
        withContext(Dispatchers.IO) {
            try {
                val dataMap = mutableMapOf(
                    "type" to notification.type,
                    "title" to notification.title,
                    "body" to notification.body
                )

                notification.boardId?.let { dataMap["boardId"] = it.toString() }
                notification.commentId?.let { dataMap["commentId"] = it.toString() }
                notification.userId?.let { dataMap["userId"] = it.toString() }
                notification.roomId?.let { dataMap["roomId"] = it }
                notification.senderId?.let { dataMap["senderId"] = it.toString() }
                notification.senderName?.let { dataMap["senderName"] = it }
                notification.senderLoginId?.let { dataMap["senderLoginId"] = it }
                notification.senderProfileImagePath?.let { dataMap["senderProfileImagePath"] = it }

                val androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setTtl(86400 * 1000)
                    .build()

                val message = Message.builder()
                    .setToken(token)
                    .putAllData(dataMap)
                    .setAndroidConfig(androidConfig)
                    .build()

                val response = FirebaseMessaging.getInstance().send(message)
                logger.info("Successfully sent notification: $response")
            } catch (e: Exception) {
                logger.error("Error sending notification to token $token: ${e.message}", e)
            }
        }
    }

    override suspend fun sendMulticast(tokens: List<String>, notification: FCMNotification) {
        withContext(Dispatchers.IO) {
            try {
                if (tokens.isEmpty()) {
                    logger.info("No tokens provided to send multicast")
                    return@withContext
                }

                val androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setTtl(86400 * 1000)
                    .build()

                var successCount = 0
                var failureCount = 0

                tokens.forEach { token ->
                    try {
                        val dataMap = mutableMapOf(
                            "type" to notification.type,
                            "title" to notification.title,
                            "body" to notification.body
                        )

                        notification.boardId?.let { dataMap["boardId"] = it.toString() }
                        notification.commentId?.let { dataMap["commentId"] = it.toString() }
                        notification.userId?.let { dataMap["userId"] = it.toString() }
                        notification.roomId?.let { dataMap["roomId"] = it }
                        notification.senderId?.let { dataMap["senderId"] = it.toString() }
                        notification.senderName?.let { dataMap["senderName"] = it }
                        notification.senderLoginId?.let { dataMap["senderLoginId"] = it }
                        notification.senderProfileImagePath?.let { dataMap["senderProfileImagePath"] = it }

                        val message = Message.builder()
                            .setToken(token)
                            .putAllData(dataMap)
                            .setAndroidConfig(androidConfig)
                            .build()

                        val response = FirebaseMessaging.getInstance().send(message)
                        successCount++
                        logger.debug("Successfully sent message: $response")
                    } catch (e: Exception) {
                        failureCount++
                        logger.error("Failed to send message to token $token: ${e.message}")
                    }
                }

                logger.info("Sent $successCount messages successfully, $failureCount failed")
            } catch (e: Exception) {
                logger.error("Error sending multicast notification: ${e.message}", e)
            }
        }
    }
}