package com.ninezero.models.tables

object SavedBoards : BaseTable("saved_boards") {
    val boardId = long("board_id").references(Boards.id)
    val createUserId = long("create_user_id").references(Users.id)

    init {
        uniqueIndex("unique_saved_board", boardId, createUserId)
    }
}