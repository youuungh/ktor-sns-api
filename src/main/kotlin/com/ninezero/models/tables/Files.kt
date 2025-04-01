package com.ninezero.models.tables

object Files : BaseTable("files") {
    val fileName = varchar("file_name", 255)
    val filePath = varchar("file_path", 255)
    val createUserId = long("create_user_id").references(Users.id)
}