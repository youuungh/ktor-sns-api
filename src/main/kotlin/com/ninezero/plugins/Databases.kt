package com.ninezero.plugins

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoDatabase
import com.ninezero.models.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

fun Application.configureDatabases() {
    val logger = LoggerFactory.getLogger("Database")
    val config = HikariConfig().apply {
        driverClassName = environment.config.property("database.driverClassName").getString()
        jdbcUrl = environment.config.property("database.url").getString()
        username = environment.config.property("database.user").getString()
        password = environment.config.property("database.password").getString()
        maximumPoolSize = 10
    }

    // 데이터베이스 설정
    try {
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        logger.info("Database connected success")
    } catch (e: Exception) {
        logger.error("Database connection Failed: ${e.message}")
    }

    // SQL 로깅 설정
    transaction {
        addLogger(StdOutSqlLogger)

        if (environment.config.property("exposed.show-sql").getString().toBoolean()) {
            addLogger(Slf4jSqlDebugLogger)
        }

        // 테이블 생성
//        SchemaUtils.drop(
//            Users,
//            Files,
//            Boards,
//            Comments,
//            LikeBoards,
//            Follows,
//            SavedBoards,
//            RecentSearches,
//            DeviceTokens,
//            Notifications
//        )
        SchemaUtils.create(
            Users,
            Files,
            Boards,
            Comments,
            LikeBoards,
            Follows,
            SavedBoards,
            RecentSearches,
            DeviceTokens,
            Notifications
        )

        // 테이블 생성 확인 로깅
        logger.info("Tables created successfully")
        logger.info("Users columns: ${Users.columns.joinToString(", ") { it.name }}")
    }
}

/** H2 DB
fun Application.configureDatabases() {
val logger = LoggerFactory.getLogger("Database")

// H2 Database 설정
val database = Database.connect(
url = environment.config.property("database.url").getString(),
driver = environment.config.property("database.driverClassName").getString(),
user = environment.config.property("database.user").getString(),
password = environment.config.property("database.password").getString()
)

// SQL 로깅 설정
transaction(database) {
addLogger(StdOutSqlLogger)

if (environment.config.property("exposed.show-sql").getString().toBoolean()) {
addLogger(Slf4jSqlDebugLogger)
}

// 테이블 생성
//        SchemaUtils.drop(
//            Users,
//            Files,
//            Boards,
//            Comments,
//            LikeBoards,
//            Follows,
//            SavedBoards,
//            RecentSearches,
//            DeviceTokens,
//            Notifications
//        )
SchemaUtils.create(
Users,
Files,
Boards,
Comments,
LikeBoards,
Follows,
SavedBoards,
RecentSearches,
DeviceTokens,
Notifications
)

// 테이블 생성 확인 로깅
logger.info("Tables created successfully")
logger.info("Users columns: ${Users.columns.joinToString(", ") { it.name }}")
}
}
 **/

fun Application.connectToMongoDB(databaseName: String): MongoDatabase {
    val logger = LoggerFactory.getLogger("MongoDB")

    val mongoUri = environment.config.property("db.mongo.uri").getString()

    val settings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(mongoUri))
        .writeConcern(WriteConcern.MAJORITY)
        .applyToClusterSettings { builder ->
            builder.serverSelectionTimeout(30000, TimeUnit.MILLISECONDS)
            builder.localThreshold(15, TimeUnit.MILLISECONDS)
        }
        .applyToConnectionPoolSettings { builder ->
            builder.maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS)
            builder.maxConnectionLifeTime(120000, TimeUnit.MILLISECONDS)
        }
        .applyToServerSettings { builder ->
            builder.heartbeatFrequency(30000, TimeUnit.MILLISECONDS)
        }
        .build()

    logger.info("Connecting to MongoDB...")

    val mongoClient = MongoClients.create(settings)
    val database = mongoClient.getDatabase(databaseName)

    monitor.subscribe(ApplicationStopped) {
        logger.info("Closing MongoDB connection")
        mongoClient.close()
    }

    return database
}