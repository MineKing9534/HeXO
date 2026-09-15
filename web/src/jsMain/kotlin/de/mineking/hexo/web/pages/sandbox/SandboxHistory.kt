package de.mineking.hexo.web.pages.sandbox

import androidx.compose.runtime.mutableStateListOf
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.web.board.SandboxBoardViewManager

internal class SandboxHistory(private val delegate: SandboxBoardViewManager) : SandboxBoardViewManager by delegate {
    private var undoBoards = mutableStateListOf<Board>()
    private var redoBoards = mutableStateListOf<Board>()
    private var editing = false

    override var board: Board
        get() = delegate.board
        set(value) = transaction { delegate.board = value }

    fun transaction(action: () -> Unit) {
        if (editing) return action()

        val before = board.copy()
        editing = true

        try {
            action()
        } finally {
            editing = false
            if (board != before) {
                undoBoards += before
                redoBoards.clear()
            }
        }
    }

    override fun updateCell(coordinate: CellCoordinate, override: CellOverride) = transaction {
        delegate.updateCell(coordinate, override)
    }

    override fun apply(interaction: BoardInteraction.HighlightBoardInteraction) = transaction {
        delegate.apply(interaction)
    }

    override fun clearHighlights() = transaction {
        delegate.clearHighlights()
    }

    fun canUndo() = undoBoards.isNotEmpty()

    fun undo() {
        val previous = undoBoards.removeLastOrNull() ?: return
        redoBoards += board.copy()
        delegate.board = previous.copy()
    }

    fun canRedo() = redoBoards.isNotEmpty()

    fun redo() {
        val next = redoBoards.removeLastOrNull() ?: return
        undoBoards += board.copy()
        delegate.board = next.copy()
    }
}
