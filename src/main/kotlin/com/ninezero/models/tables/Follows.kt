package com.ninezero.models.tables

object Follows : BaseTable("follows") {
    val followerId = long("follower_id").references(Users.id)
    val followingId = long("following_id").references(Users.id)

    init {
        uniqueIndex("unique_follow", followerId, followingId)
    }
}