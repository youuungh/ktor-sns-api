package com.ninezero.models.tables

import org.jetbrains.exposed.sql.javatime.datetime

object RecentSearches : BaseTable("recent_searches") {
    val userId = long("user_id").references(Users.id)
    val searchedUserId = long("searched_user_id").references(Users.id)
    val searchedUserName = varchar("searched_user_name", 50)
    val searchedUserProfileImagePath = varchar("searched_user_profile_path", 255).nullable()
    val searchedAt = datetime("searched_at")

    init {
        index(true, userId, searchedUserId)
    }
}