package com.ninezero.models.mappers

import com.ninezero.models.dto.User
import com.ninezero.models.dto.UserResponse
import com.ninezero.models.tables.Users.createdAt
import com.ninezero.models.tables.Users.extraUserInfo
import com.ninezero.models.tables.Users.id
import com.ninezero.models.tables.Users.loginId
import com.ninezero.models.tables.Users.userName
import com.ninezero.models.tables.Users.password
import com.ninezero.models.tables.Users.profileImagePath
import com.ninezero.models.tables.Users.updatedAt
import org.jetbrains.exposed.sql.ResultRow

object UserMapper {
    fun toUser(row: ResultRow) = User(
        id = row[id].value,
        loginId = row[loginId],
        userName = row[userName],
        password = row[password],
        extraUserInfo = row[extraUserInfo],
        profileImagePath = row[profileImagePath],
        createdAt = row[createdAt],
        updatedAt = row[updatedAt]
    )

    fun toUserResponse(
        row: ResultRow,
        boardCount: Int = 0,
        followerCount: Int = 0,
        followingCount: Int = 0,
        isFollowing: Boolean = false
    ) = UserResponse(
        id = row[id].value,
        loginId = row[loginId],
        userName = row[userName],
        extraUserInfo = row[extraUserInfo],
        profileImagePath = row[profileImagePath],
        boardCount = boardCount,
        followerCount = followerCount,
        followingCount = followingCount,
        isFollowing = isFollowing,
        createdAt = row[createdAt],
        updatedAt = row[updatedAt]
    )
}