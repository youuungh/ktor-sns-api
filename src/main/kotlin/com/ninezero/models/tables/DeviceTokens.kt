package com.ninezero.models.tables

import org.jetbrains.exposed.sql.ReferenceOption

object DeviceTokens : BaseTable("device_tokens") {
    val userId = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 255)
    val deviceInfo = varchar("device_info", 255).nullable()
    val isActive = bool("is_active").default(true)

    init {
        uniqueIndex("unique_user_token", userId, token)
    }
}