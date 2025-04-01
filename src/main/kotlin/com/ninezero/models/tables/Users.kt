package com.ninezero.models.tables

object Users : BaseTable("users") {
    val loginId = varchar("login_id", 50).uniqueIndex()
    val userName = varchar("username", 50)
    val password = varchar("password", 128)
    val extraUserInfo = text("extra_user_info").nullable()
    val profileImagePath = varchar("profile_image_path", 255).nullable()
}