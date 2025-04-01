package com.ninezero.models.tables

object LikeBoards : BaseTable("like_boards") {
    val boardId = long("board_id").references(Boards.id)
    val createUserId = long("create_user_id").references(Users.id)
}