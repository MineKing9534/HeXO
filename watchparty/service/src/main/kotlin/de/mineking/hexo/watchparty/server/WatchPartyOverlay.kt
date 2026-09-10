package de.mineking.hexo.watchparty.server

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.watchparty.model.WatchPartyConnectionId

internal class WatchPartyOverlay {
    private val cells = mutableMapOf<CellCoordinate, AuthoredCellHighlight>()
    private val lines = mutableListOf<AuthoredLineHighlight>()

    context(author: WatchPartyConnectionId)
    fun replace(board: Board) {
        cells.clear()
        lines.clear()

        board.cells.forEach { (coordinate, cell) ->
            updateCell(coordinate, cell.highlight)
        }

        board.lineHighlights.forEach {
            highlightLine(it, remove = false)
        }
    }

    context(author: WatchPartyConnectionId)
    fun updateCell(coordinate: CellCoordinate, highlight: CellHighlight?) {
        if (highlight == null) {
            cells.remove(coordinate)
        } else {
            cells[coordinate] = AuthoredCellHighlight(highlight, author)
        }
    }

    context(author: WatchPartyConnectionId)
    fun highlightLine(line: LineHighlight, remove: Boolean) {
        if (!remove) {
            lines.add(AuthoredLineHighlight(line, author))
        } else {
            val index = lines.indexOfLast { it.line == line }
            if (index >= 0) lines.removeAt(index)
        }
    }

    context(author: WatchPartyConnectionId)
    fun clearHighlights() {
        cells.entries.removeIf { it.value.author == author }
        lines.removeAll { it.author == author }
    }

    context(author: WatchPartyConnectionId)
    fun hasHighlights() = cells.values.any { it.author == author } || lines.any { it.author == author }

    fun toBoard() = Board(
        cells = cells.mapValues { (_, highlight) -> Cell(highlight = highlight.highlight) },
        lineHighlights = lines.map { it.line },
    )
}

private data class AuthoredCellHighlight(
    val highlight: CellHighlight,
    val author: WatchPartyConnectionId,
)

private data class AuthoredLineHighlight(
    val line: LineHighlight,
    val author: WatchPartyConnectionId,
)
