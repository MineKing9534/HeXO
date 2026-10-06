@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.board.render.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.PartialGameStateHistory
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.mutable
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.times
import de.mineking.hexo.board.toGamePosition

object CombinedNotationBoardRenderer : BoardRenderer<Unit, String> {
    override suspend fun render(board: Board, param: Unit) = board.renderCombinedNotation()
}

@OptIn(InternalBoardApi::class)
fun Board.renderCombinedNotation(
    separator: String = "---",
    originLabel: String = "*",
): String {
    val position = toGamePosition()

    val origin = position.findBKEOrigin()
    val state = position.state.mutable().apply {
        this[origin].label = originLabel
    }

    val stateNotation = state.renderRectilinearNotation(RectilinearNotationType.Compact)
    val turnsNotation = position.turns.renderBKENotation(origin = origin)

    return "$stateNotation $separator $turnsNotation"
}

private fun PartialGameStateHistory.findBKEOrigin(): CellCoordinate {
    val moves = turns.moves.map { it.coordinate }
    val moveCoordinates = moves.toSet()

    val coordinateOrder = compareBy<CellCoordinate>({ it.q }, { it.r })
    val references = moves.ifEmpty { state.cells.keys.toList() }

    state.cells.asSequence()
        .filter { (coordinate, cell) ->
            coordinate !in moveCoordinates && cell.label.isBlank() && !cell.isEmpty(includeHighlights = true)
        }
        .map { it.key }
        .minWithOrNull(compareBy<CellCoordinate>(
            { candidate -> references.maxOfOrNull { candidate.distanceTo(it) } ?: 0 },
            { candidate -> references.sumOf { candidate.distanceTo(it).toLong() } },
        ).then(coordinateOrder))
        ?.let { return it }

    val anchor = state.cells.keys.minWithOrNull(coordinateOrder)
        ?: moves.firstOrNull()
        ?: CellCoordinate.Zero
    val unavailableCoordinates = state.cells.keys + moveCoordinates

    // Search outward along the six rays without replacing initial cells or later moves.
    return generateSequence(1) { it + 1 }
        .flatMap { ring -> Direction.entries.asSequence().map { anchor + it.direction * ring } }
        .first { it !in unavailableCoordinates }
}
