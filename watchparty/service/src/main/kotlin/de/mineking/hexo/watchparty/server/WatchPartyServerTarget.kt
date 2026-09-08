package de.mineking.hexo.watchparty.server

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.hasHighlights
import de.mineking.hexo.board.plusAssign
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.utils.types.Omissible
import de.mineking.hexo.watchparty.protocol.WatchPartyTargetDto

internal sealed interface WatchPartyServerTarget {
    fun toDto(): WatchPartyTargetDto

    context(author: WatchPartyConnectionId)
    fun updateBoard(board: Board)

    context(author: WatchPartyConnectionId)
    fun updateCell(coordinate: CellCoordinate, cell: CellOverride)

    context(author: WatchPartyConnectionId)
    fun highlightLine(line: LineHighlight, remove: Boolean)

    fun setMove(move: Int): Unit = throw WatchPartyRequestException("invalid watchparty target")

    fun undo(): Unit = throw WatchPartyRequestException("invalid watchparty target")
    fun redo(): Unit = throw WatchPartyRequestException("invalid watchparty target")

    context(author: WatchPartyConnectionId)
    fun transaction(edits: List<WatchPartyEdit>): Unit =
        throw WatchPartyRequestException("invalid sandbox transaction")

    context(connectionId: WatchPartyConnectionId)
    fun hasClearableHighlights(): Boolean

    context(connectionId: WatchPartyConnectionId)
    fun clearHighlights()

    context(connectionId: WatchPartyConnectionId)
    fun handleDisconnect() = Unit

    abstract class AbstractGameTarget : WatchPartyServerTarget {
        protected val overlay = WatchPartyOverlay()
        protected var currentMove = Int.MAX_VALUE

        context(author: WatchPartyConnectionId)
        override fun updateBoard(board: Board) = overlay.replace(board)

        context(author: WatchPartyConnectionId)
        override fun updateCell(coordinate: CellCoordinate, cell: CellOverride) {
            val highlight = cell.highlight
            if (highlight is Omissible.Present) overlay.updateCell(coordinate, highlight.value)
        }

        context(author: WatchPartyConnectionId)
        override fun highlightLine(line: LineHighlight, remove: Boolean) {
            overlay.highlightLine(line, remove)
        }

        override fun setMove(move: Int) {
            currentMove = move
        }

        context(connectionId: WatchPartyConnectionId)
        override fun hasClearableHighlights() = overlay.hasHighlights()

        context(connectionId: WatchPartyConnectionId)
        override fun clearHighlights() = overlay.clearHighlights()

        context(connectionId: WatchPartyConnectionId)
        override fun handleDisconnect() = clearHighlights()
    }

    class Session(val sessionId: SessionId) : AbstractGameTarget() {
        override fun toDto() = WatchPartyTargetDto.Session(sessionId, currentMove, overlay.toBoard())
    }

    class Game(val gameId: GameId) : AbstractGameTarget() {
        override fun toDto() = WatchPartyTargetDto.Game(gameId, currentMove, overlay.toBoard())
    }

    class Sandbox : WatchPartyServerTarget {
        private val history = SandboxBoardHistory()

        override fun toDto() = WatchPartyTargetDto.Sandbox(history.board.copy(), canUndo = history.canUndo, canRedo = history.canRedo)

        context(connectionId: WatchPartyConnectionId)
        override fun hasClearableHighlights() = history.board.hasHighlights()

        context(author: WatchPartyConnectionId)
        override fun updateBoard(board: Board) = history.replace(board)

        context(author: WatchPartyConnectionId)
        override fun updateCell(coordinate: CellCoordinate, cell: CellOverride) = history.transaction {
            board[coordinate] += cell
        }

        context(author: WatchPartyConnectionId)
        override fun highlightLine(line: LineHighlight, remove: Boolean) = history.transaction {
            if (remove) {
                board.lineHighlights -= line
            } else {
                board.lineHighlights += line
            }
        }

        context(connectionId: WatchPartyConnectionId)
        override fun clearHighlights() = history.transaction {
            board.lineHighlights.clear()
            board.cells.values.forEach { it.highlight = null }
        }

        override fun undo() = history.undo()
        override fun redo() = history.redo()

        context(author: WatchPartyConnectionId)
        override fun transaction(edits: List<WatchPartyEdit>) = history.transaction {
            edits.forEach {
                it.apply(this@Sandbox)
            }
        }
    }
}
