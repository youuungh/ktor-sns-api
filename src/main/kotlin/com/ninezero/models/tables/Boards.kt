package com.ninezero.models.tables

object Boards : BaseTable("boards") {
    val title = varchar("title", 255)
    val content = text("content")
    val createUserId = long("create_user_id").references(Users.id)
    val createUserName = varchar("create_user_name", 50)
    val createUserProfileImagePath = varchar("create_user_profile_path", 255).nullable()
    val updateUserId = long("update_user_id").references(Users.id)
    val updateUserName = varchar("update_user_name", 50)
    val updateUserProfileImagePath = varchar("update_user_profile_path", 255).nullable()
}