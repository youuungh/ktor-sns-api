package com.ninezero.models.mappers

import com.ninezero.models.dto.BoardResponse
import com.ninezero.models.dto.CommentResponse
import com.ninezero.models.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList

object BoardMapper {
    private const val DEFAULT_COMMENT_COUNT = 0
    private const val DEFAULT_LIKES_COUNT = 0
    private val DEFAULT_COMMENTS = emptyList<CommentResponse>()

    fun toBoardResponse(row: ResultRow, isLiked: Boolean = false, requesterId: Long): BoardResponse {
        return toBoardResponseList(listOf(row), mapOf(row[Boards.id].value to isLiked), requesterId).first()
    }

    fun toBoardResponseList(
        boards: List<ResultRow>,
        isLikedMap: Map<Long, Boolean> = emptyMap(),
        requesterId: Long
    ): List<BoardResponse> {
        if (boards.isEmpty()) return emptyList()

        val boardIds = boards.map { it[Boards.id].value }
        val createUserIds = boards.map { it[Boards.createUserId] }

        // 댓글 조회
        val parentComments = Comments.alias("parent")
        val comments = Comments
            .join(parentComments, JoinType.LEFT, additionalConstraint =
                { Comments.parentId eq parentComments[Comments.id] })
            .select(Comments.columns + parentComments[Comments.createUserName])
            .where { Comments.boardId inList boardIds }
            .orderBy(Comments.createdAt to SortOrder.ASC)

        val commentsMap = comments
            .groupBy { it[Comments.boardId] }
            .mapValues { (_, rows) ->
                rows.map { row ->
                    val parentUserName = row.getOrNull(parentComments[Comments.createUserName])
                    CommentMapper.toCommentResponse(row, parentUserName)
                }
            }

        // 댓글 수 조회
        val commentCountMap = Comments
            .select(Comments.boardId, Comments.id.count())
            .where { Comments.boardId inList boardIds }
            .groupBy(Comments.boardId)
            .associate { row ->
                row[Comments.boardId] to row[Comments.id.count()].toInt()
            }

        // 좋아요 수 조회
        val likesCountMap = LikeBoards
            .select(LikeBoards.boardId, LikeBoards.id.count())
            .where { LikeBoards.boardId inList boardIds }
            .groupBy(LikeBoards.boardId)
            .associate { row ->
                row[LikeBoards.boardId] to row[LikeBoards.id.count()].toInt()
            }

        // 유저 팔로잉 상태 조회
        val followingMap = Follows
            .select(Follows.followingId)
            .where {
                (Follows.followerId eq requesterId) and
                        (Follows.followingId inList createUserIds)
            }
            .map { it[Follows.followingId] }
            .toSet()

        // 저장 상태 조회
        val savedMap = SavedBoards
            .select(SavedBoards.boardId)
            .where {
                (SavedBoards.createUserId eq requesterId) and
                        (SavedBoards.boardId inList boardIds)
            }
            .map { it[SavedBoards.boardId] }
            .toSet()

        return boards.map { row ->
            val boardId = row[Boards.id].value
            val createUserId = row[Boards.createUserId]
            BoardResponse(
                id = boardId,
                title = row[Boards.title],
                content = row[Boards.content],
                createdAt = row[Boards.createdAt],
                updatedAt = row[Boards.updatedAt],
                createUserId = createUserId,
                createUserName = row[Boards.createUserName],
                createUserProfileImagePath = row[Boards.createUserProfileImagePath],
                updateUserId = row[Boards.updateUserId],
                updateUserName = row[Boards.updateUserName],
                updateUserProfileImagePath = row[Boards.updateUserProfileImagePath],
                comments = commentsMap[boardId] ?: DEFAULT_COMMENTS,
                commentCount = commentCountMap[boardId] ?: DEFAULT_COMMENT_COUNT,
                likesCount = likesCountMap[boardId] ?: DEFAULT_LIKES_COUNT,
                isLiked = isLikedMap[boardId] ?: false,
                isFollowing = followingMap.contains(createUserId),
                isSaved = savedMap.contains(boardId)
            )
        }
    }
}