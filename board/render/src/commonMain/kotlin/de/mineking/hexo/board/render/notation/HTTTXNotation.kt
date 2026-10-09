@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.board.render.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.toGamePosition

object HTTTXNotationBoardRenderer : BoardRenderer<Unit, String> {
    override suspend fun render(board: Board, param: Unit) = board
        .renderHTTTXNotation()
}

fun Board.renderHTTTXNotation(lineSeparator: String = "\n"): String {
    var requiresVersion2 = false
    var requiresExtension = false

    val body = buildString {
        val (state, turns) = toGamePosition()

        fun appendMove(coordinate: CellCoordinate) {
            append("[")
            appendCoordinate(coordinate)
            append("]")
        }

        fun appendState(owner: CellOwner) {
            append(owner.symbol)

            val cells = state.cells.filterValues { it.owner == owner }
            if (cells.isNotEmpty()) {
                append(" ")
                cells.forEach { (coordinate) -> appendMove(coordinate) }
            }
        }

        if (!state.isEmpty(includeHighlights = true) || turns.turns.isEmpty()) {
            requiresVersion2 = true

            val firstPlayer = turns.turns.firstOrNull()?.meta?.player ?: CellOwner.X

            appendState(firstPlayer)
            append(" : ")
            appendState(firstPlayer.other)

            appendVisuals(state)
            append(";$lineSeparator")
        }

        val actualTurns = turns.turns.dropWhile { it.moves.first().coordinate == CellCoordinate.Zero }
        actualTurns.forEachIndexed { index, turn ->
            append("${index + 1}. ")
            turn.moves.forEach {
                appendMove(it.coordinate)
            }

            if (turn.moves.size != 2) requiresExtension = true

            append(";$lineSeparator")
        }
    }

    val version = if (requiresVersion2 || requiresExtension) "2" else "1"
    val extension = if (requiresExtension) "u" else ""

    return "version[$version$extension];$lineSeparator$body"
}

private fun StringBuilder.appendVisuals(board: Board) {
    board.cells.forEach { (coordinate, cell) ->
        val label = cell.label.takeIf { it.isNotBlank() }
        val highlight = cell.highlight

        if (label == null && highlight == null) return@forEach

        append(" <")
        appendCoordinate(coordinate)

        if (highlight != null) {
            append(":#")
            append(highlight.color?.symbol ?: "")
        }

        if (label != null) {
            append(":$")
            append(label.escape())
        }

        append(">")
    }
}

private val SPECIAL_CHARACTERS = setOf('>', ':', '#', '$')
private fun String.escape() = toCharArray().joinToString("") {
    if (it in SPECIAL_CHARACTERS) "\\$it" else "$it"
}

private fun StringBuilder.appendCoordinate(coordinate: CellCoordinate) {
    append(coordinate.q + coordinate.r)
    append(",")
    append(-coordinate.r)
}
