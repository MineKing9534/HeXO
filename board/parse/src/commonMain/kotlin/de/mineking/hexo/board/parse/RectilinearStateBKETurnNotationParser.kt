package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.mutable
import de.mineking.hexo.board.plus

object RectilinearStateBKETurnNotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseRectilinearStateBKETurnNotation(focusWinningRows = false)
}

@OptIn(InternalBoardApi::class)
fun String.parseRectilinearStateBKETurnNotation(focusWinningRows: Boolean = true): Board {
    val parts = splitStateAndTurns()

    val board = if (parts.size == 1) {
        try {
            parseExtendedBKENotation(implicitOrigin = true, focusWinningRows = false)
        } catch (_: HexoNotationFormatException) {
            parseRectilinearNotation(focusWinningRows = false)
        }
    } else {
        val (rectilinear, bke) = parts

        val originalState = rectilinear.parseRectilinearNotation(focusWinningRows = false)
        val additionalMoves = bke.parseExtendedBKENotation(implicitOrigin = false, focusWinningRows = false)

        originalState + additionalMoves
    }

    if (focusWinningRows) {
        return board.mutable().focusWinningRows()
    }

    return board
}

private fun String.splitStateAndTurns(): List<String> {
    var labelDepth = 0
    var escaped = false
    forEachIndexed { index, character ->
        when {
            escaped -> escaped = false
            character == '\\' -> escaped = true
            character == '[' -> labelDepth++
            character == ']' && labelDepth > 0 -> labelDepth--
            character == ',' && labelDepth == 0 -> {
                return listOf(
                    substring(0, index),
                    substring(index + 1).trim(),
                )
            }
        }
    }
    return listOf(this)
}
