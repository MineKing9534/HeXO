package de.mineking.hexo.watchparty.server

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.LineHighlight

internal sealed interface WatchPartyEdit {
    context(author: WatchPartyConnectionId)
    fun apply(target: WatchPartyServerTarget)

    data class ReplaceBoard(val board: Board) : WatchPartyEdit {
        context(author: WatchPartyConnectionId)
        override fun apply(target: WatchPartyServerTarget) = target.updateBoard(board)
    }

    data class UpdateCell(val coordinate: CellCoordinate, val cell: CellOverride) : WatchPartyEdit {
        context(author: WatchPartyConnectionId)
        override fun apply(target: WatchPartyServerTarget) = target.updateCell(coordinate, cell)
    }

    data class HighlightLine(val line: LineHighlight, val remove: Boolean) : WatchPartyEdit {
        context(author: WatchPartyConnectionId)
        override fun apply(target: WatchPartyServerTarget) = target.highlightLine(line, remove)
    }

    data object ClearHighlights : WatchPartyEdit {
        context(author: WatchPartyConnectionId)
        override fun apply(target: WatchPartyServerTarget) = target.clearHighlights()
    }
}
