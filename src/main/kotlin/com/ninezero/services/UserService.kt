package com.ninezero.services

import com.ninezero.models.dto.*

interface UserService {
    suspend fun create(request: UserCreateRequest): Long
    suspend fun authenticate(request: LoginRequest): UserResponse
    suspend fun authenticateSocial(request: SocialLoginRequest): UserResponse
    suspend fun getAllUsers(page: Int, size: Int, requesterId: Long): List<UserResponse>
    suspend fun getById(id: Long, requesterId: Long): UserResponse
    suspend fun update(id: Long, request: UserUpdateRequest): UserResponse
    suspend fun follow(followerId: Long, followingId: Long): Long
    suspend fun unfollow(followerId: Long, followingId: Long): Long

    // search
    suspend fun searchUsers(query: String, page: Int, size: Int, requesterId: Long): List<UserResponse>
    suspend fun saveRecentSearch(userId: Long, searchedUserId: Long)
    suspend fun getRecentSearches(userId: Long, limit: Int = 10): List<RecentSearch>
    suspend fun deleteRecentSearch(userId: Long, searchedUserId: Long)
    suspend fun clearRecentSearches(userId: Long)
}