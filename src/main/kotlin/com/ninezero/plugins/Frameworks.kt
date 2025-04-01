package com.ninezero.plugins

import com.mongodb.reactivestreams.client.MongoDatabase
import com.mongodb.reactivestreams.client.gridfs.GridFSBuckets
import com.ninezero.services.*
import com.ninezero.services.social.GoogleLoginProvider
import com.ninezero.services.social.KakaoLoginProvider
import com.ninezero.services.social.NaverLoginProvider
import com.ninezero.services.social.SocialLoginProviderManager
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(serviceModule(this@configureFrameworks))
    }
}

private fun serviceModule(application: Application) = module {
    // File Upload MongoDB
    single {
        val dbName = application.environment.config.property("db.mongo.database.name").getString()
        application.connectToMongoDB(dbName)
    }

    // Chat MongoDB
    single(named("chatDatabase")) {
        val chatDbName = application.environment.config.property("db.mongo.chat.database.name").getString()
        application.connectToMongoDB(chatDbName)
    }

    // GridFs
    single {
        val database = get<MongoDatabase>()
        val bucketName = application.environment.config.property("db.mongo.gridfs.bucket").getString()
        val chunkSize = application.environment.config.property("db.mongo.gridfs.chunkSize").getString().toInt()

        GridFSBuckets.create(database, bucketName)
            .withChunkSizeBytes(chunkSize)
    }

    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                level = LogLevel.HEADERS
            }
        }
    }

    // FCM Service
    single<FCMService> {
        val fcmService = FCMServiceImpl(application.environment.config)
        fcmService.initialize()
        fcmService
    }

    // Notification Service
    single<NotificationService> { NotificationServiceImpl(get()) }

    // Social Login Service
    single { GoogleLoginProvider() }
    single { NaverLoginProvider(get()) }
    single { KakaoLoginProvider(get()) }

    single {
        SocialLoginProviderManager(
            listOf(
                get<GoogleLoginProvider>(),
                get<NaverLoginProvider>(),
                get<KakaoLoginProvider>()
            )
        )
    }

    // Services
    single<FileService> { FileServiceImpl(get()) }
    single<UserService> {
        UserServiceImpl(
            fileService = get(),
            database = get(named("chatDatabase")),
            notificationService = get(),
            socialLoginProviderManager = get()
        )
    }
    single<BoardService> { BoardServiceImpl(get(), get()) }
    single<ChatService> { ChatServiceImpl(get(named("chatDatabase")), get()) }
}

/**
fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(serviceModule(environment.config))
    }
}

private fun serviceModule(config: ApplicationConfig) = module {
    single { config }
    single { GridFSBuckets.create(get<MongoDatabase>()) }
    single<UserService> { UserServiceImpl(get()) }
    single<BoardService> { BoardServiceImpl(get()) }
    single<FileService> { FileServiceImpl(get()) }
}
*/