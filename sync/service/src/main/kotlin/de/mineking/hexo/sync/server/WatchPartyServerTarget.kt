package de.mineking.hexo.sync.server

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.hasHighlights
import de.mineking.hexo.hds.game.GameId
import de.mineking.hexo.hds.session.SessionId
import de.mineking.hexo.sync.common.WatchPartyData
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.sync.common.WatchPartyTarget

internal sealed interface WatchPartyServerTarget {
    fun toDto(): WatchPartyTarget
    fun hasClearableHighlights(connectionId: WatchPartyConnectionId): Boolean
    fun clearHighlightsBy(connectionId: WatchPartyConnectionId): WatchPartyServerTarget

    fun handleDisconnect(connectionId: WatchPartyConnectionId): WatchPartyServerTarget = this

    abstract class AbstractGameTarget : WatchPartyServerTarget {
        abstract val overlay: WatchPartyOverlay
        abstract fun copy(overlay: WatchPartyOverlay): AbstractGameTarget

        override fun hasClearableHighlights(connectionId: WatchPartyConnectionId) = overlay.hasHighlightBy(connectionId)
        override fun clearHighlightsBy(connectionId: WatchPartyConnectionId) = copy(overlay = overlay.clearHighlightsBy(connectionId))
        override fun handleDisconnect(connectionId: WatchPartyConnectionId) = clearHighlightsBy(connectionId)
    }

    data class Session(
        val sessionId: SessionId,
        val move: Int,
        override val overlay: WatchPartyOverlay = WatchPartyOverlay(),
    ) : AbstractGameTarget() {
        override fun copy(overlay: WatchPartyOverlay) = copy(overlay = overlay, move = move)

        override fun toDto() = WatchPartyTarget.Session(
            sessionId = sessionId,
            move = move,
            overlay = overlay.toBoard(),
        )
    }

    data class Game(
        val gameId: GameId,
        val move: Int,
        override val overlay: WatchPartyOverlay = WatchPartyOverlay(),
    ) : AbstractGameTarget() {
        override fun copy(overlay: WatchPartyOverlay) = copy(overlay = overlay, move = move)

        override fun toDto() = WatchPartyTarget.Game(
            gameId = gameId,
            move = move,
            overlay = overlay.toBoard(),
        )
    }

    data class Sandbox(
        val board: Board,
    ) : WatchPartyServerTarget {
        override fun toDto() = WatchPartyTarget.Sandbox(board)

        override fun hasClearableHighlights(connectionId: WatchPartyConnectionId) = board.hasHighlights()
        override fun clearHighlightsBy(connectionId: WatchPartyConnectionId) = copy(board = board.clearHighlights())
    }
}

internal data class AuthoredCellHighlight(
    val highlight: CellHighlight,
    val author: WatchPartyConnectionId,
)

internal data class AuthoredLineHighlight(
    val line: LineHighlight,
    val author: WatchPartyConnectionId,
)

internal data class WatchPartyOverlay(
    val cells: Map<CellCoordinate, AuthoredCellHighlight> = emptyMap(),
    val lines: List<AuthoredLineHighlight> = emptyList(),
) {
    companion object {
        fun fromBoard(board: Board, author: WatchPartyConnectionId) = WatchPartyOverlay(
            cells = board.cells
                .mapNotNull { (coordinate, cell) ->
                    val highlight = cell.highlight ?: return@mapNotNull null
                    coordinate to AuthoredCellHighlight(highlight, author)
                }
                .toMap(),
            lines = board.lineHighlights.map { AuthoredLineHighlight(it, author) },
        )
    }

    fun updateCell(coordinate: CellCoordinate, highlight: CellHighlight?, author: WatchPartyConnectionId) = copy(
        cells = if (highlight == null) {
            cells - coordinate
        } else {
            cells + (coordinate to AuthoredCellHighlight(highlight, author))
        },
    )

    fun addLine(line: LineHighlight, author: WatchPartyConnectionId) = copy(
        lines = lines + AuthoredLineHighlight(line, author),
    )

    fun removeLine(line: LineHighlight): WatchPartyOverlay {
        val index = lines.indexOfLast { it.line == line }
        if (index == -1) return this

        return copy(lines = lines.filterIndexed { i, _ -> i != index })
    }

    fun clearHighlightsBy(author: WatchPartyConnectionId) = copy(
        cells = cells.filterValues { it.author != author },
        lines = lines.filter { it.author != author },
    )

    fun hasHighlightBy(author: WatchPartyConnectionId) = cells.any { (_, cell) -> cell.author == author } || lines.any { it.author == author }

    fun toBoard() = Board(
        cells = cells.mapValues { (_, highlight) -> Cell(highlight = highlight.highlight) },
        lineHighlights = lines.map { it.line },
    )
}

internal data class WatchPartyState(
    val id: WatchPartyId,
    val target: WatchPartyServerTarget?,
) {
    fun toDto(connectionId: WatchPartyConnectionId) = WatchPartyData(
        id = id,
        target = target?.toDto(),
        clearableHighlights = target?.hasClearableHighlights(connectionId) ?: false,
    )
}

private fun Board.clearHighlights() = copy().apply {
    lineHighlights.clear()
    cells.values.forEach { it.highlight = null }
}
