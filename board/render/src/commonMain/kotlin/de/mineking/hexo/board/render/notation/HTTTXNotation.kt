@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.board.render.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.GamePosition
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.toGamePositionForce

object HTTTXNotationBoardRenderer : BoardRenderer<Unit, String> {
    override suspend fun render(board: Board, param: Unit) = board
        .toGamePositionForce()
        .renderHTTTXNotation()
}

fun GamePosition<*>.renderHTTTXNotation(lineSeparator: String = "\n") = buildString {
    append("version[1];$lineSeparator")

    // TODO support exporting highlights and labels

    fun appendCoordinate(coordinate: CellCoordinate) {
        append("[")
        append(coordinate.q + coordinate.r)
        append(", ")
        append(-coordinate.r)
        append("]")
    }

    val turns = turns.takeLastWhile { it.moves.first().coordinate != CellCoordinate.Zero }
    turns.forEachIndexed { index, turn ->
        append("${index + 1}. ")
        turn.moves.forEach {
            appendCoordinate(it.coordinate)
        }
        append(";$lineSeparator")
    }
}
