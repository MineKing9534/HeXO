package de.mineking.hexo.watchparty.server

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.mutable

internal class SandboxBoardHistory {
    @OptIn(InternalBoardApi::class)
    var board = Board.withTurnNumbers().mutable()
        private set

    private val past = ArrayDeque<Board>()
    private val future = ArrayDeque<Board>()
    private var editing = false

    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()

    fun replace(next: Board) = transaction { board = next.copy() }

    fun transaction(action: SandboxBoardHistory.() -> Unit) {
        if (editing) return action()

        val before = board.copy()
        editing = true

        @Suppress("TooGenericExceptionCaught")
        try {
            action()
            if (board != before) {
                past.addLast(before)
                future.clear()
            }
        } catch (e: Throwable) {
            board = before
            throw e
        } finally {
            editing = false
        }
    }

    fun undo() {
        check(!editing)

        val previous = past.removeLastOrNull() ?: return
        future += board
        board = previous.copy()
    }

    fun redo() {
        check(!editing)

        val next = future.removeLastOrNull() ?: return
        past += board
        board = next.copy()
    }
}
