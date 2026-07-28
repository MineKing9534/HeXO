@file:OptIn(InternalBoardApi::class)

package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.mutable
import de.mineking.hexo.hds.HdsApiClient

interface BoardParser {
    suspend fun parse(notation: String): Board

    companion object {
        internal object None : BoardParser {
            override suspend fun parse(notation: String) = throw NotImplementedError()
        }

        val Default = None
            .or(TytoLinkParser)
            .or(HTTTXNotationParser.allowTurnLabels())
            .or(RectilinearStateBKETurnNotationParser)

        fun createWithHdsSupport(client: HdsApiClient) = None
            .or(HdsGameLinkParser(client.finishedGameRepository).allowTurnLabels())
            .or(HdsSandboxLinkParser(client.formationRepository).allowTurnLabels())
            .or(Default)
    }
}

fun BoardParser.focusWinningRows() = object : BoardParser {
    override suspend fun parse(notation: String) = this@focusWinningRows
        .parse(notation)
        .mutable()
        .focusWinningRows()
}

fun BoardParser.or(other: BoardParser) = when (this) {
    BoardParser.Companion.None -> other
    else -> object : BoardParser {
        override suspend fun parse(notation: String) = try {
            this@or.parse(notation)
        } catch (_: HexoNotationFormatException) {
            other.parse(notation)
        }
    }
}

fun BoardParser.allowTurnLabels() = object : BoardParser {
    override suspend fun parse(notation: String): Board {
        val addTurnLabels = notation.startsWith("#")
        val notation = notation.removePrefix("#")

        val board = this@allowTurnLabels.parse(notation)
        if (!addTurnLabels) return board

        return board
            .mutable()
            .also { it.attributes[BoardAttribute.ShowTurnNumbers] = true }
    }
}
