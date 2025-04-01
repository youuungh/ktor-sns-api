package com.ninezero.services

import com.ninezero.models.dto.BoardRequest
import com.ninezero.models.dto.BoardResponse
import com.ninezero.models.dto.CommentRequest
import com.ninezero.models.dto.CommentResponse

interface BoardService {
    suspend fun create(request: BoardRequest, requesterId: Long): Long
    suspend fun getList(page: Int, size: Int, requesterId: Long): List<BoardResponse>
    suspend fun getMyList(page: Int, size: Int, requesterId: Long): List<BoardResponse>
    suspend fun getSavedList(page: Int, size: Int, requesterId: Long): List<BoardResponse>
    suspend fun getListById(userId: Long, page: Int, size: Int, requesterId: Long): List<BoardResponse>
    suspend fun update(id: Long, request: BoardRequest, requesterId: Long): Long
    suspend fun delete(id: Long, requesterId: Long): Long
    suspend fun getComments(boardId: Long, page: Int, size: Int): List<CommentResponse>
    suspend fun getReplies(boardId: Long, parentId: Long): List<CommentResponse>
    suspend fun createComment(boardId: Long, request: CommentRequest, requesterId: Long): Long
    suspend fun deleteComment(boardId: Long, commentId: Long, requesterId: Long): Long
    suspend fun like(boardId: Long, requesterId: Long): Long
    suspend fun unlike(boardId: Long, requesterId: Long): Long
    suspend fun saveBoard(boardId: Long, requesterId: Long): Long
    suspend fun unsaveBoard(boardId: Long, requesterId: Long): Long
}