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
    val parts = split(",\\s*".toRegex(), limit = 2)

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
