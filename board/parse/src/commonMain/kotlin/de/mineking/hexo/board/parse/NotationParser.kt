@file:OptIn(InternalBoardApi::class)

package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.mutable
import de.mineking.hexo.board.parse.notation.BKENotationParser
import de.mineking.hexo.board.parse.notation.CombinedNotationParser
import de.mineking.hexo.board.parse.notation.HTTTXNotationParser
import de.mineking.hexo.board.parse.notation.RectilinearNotationParser
import de.mineking.hexo.board.parse.notation.TytoLinkParser

interface NotationParser {
    suspend fun parse(notation: String): Board

    companion object {
        object None : NotationParser {
            override suspend fun parse(notation: String) = throw NotImplementedError()
        }

        val Default = None
            .or(TytoLinkParser.allowTurnLabels())
            .or(HTTTXNotationParser.allowTurnLabels())
            .or(RectilinearNotationParser)
            .or(BKENotationParser)
            .or(CombinedNotationParser.Default)
    }
}

fun NotationParser.focusWinningRows() = object : NotationParser {
    override suspend fun parse(notation: String) = this@focusWinningRows.parse(notation)
        .mutable()
        .focusWinningRows()
}

fun NotationParser.allowTurnLabels(marker: String = "#") = object : NotationParser {
    override suspend fun parse(notation: String): Board {
        val showTurnLabels = notation.startsWith(marker)
        return this@allowTurnLabels.parse(notation.removePrefix(marker))
            .mutable()
            .apply {
                if (showTurnLabels) {
                    attributes[BoardAttribute.ShowTurnNumbers] = true
                }
            }
    }
}

infix fun NotationParser.or(other: NotationParser) = when (this) {
    NotationParser.Companion.None -> other
    else -> object : NotationParser {
        override suspend fun parse(notation: String) = try {
            this@or.parse(notation)
        } catch (_: HexoNotationFormatException) {
            other.parse(notation)
        }
    }
}

abstract class LinkParser(private val prefix: String) : NotationParser {
    final override suspend fun parse(notation: String): Board {
        val trimmedNotation = notation.trim()

        if (!trimmedNotation.startsWith(prefix)) throw HexoNotationFormatException("Invalid link")

        val param = trimmedNotation.substring(startIndex = prefix.length)
        if (""".*\s.*""".toRegex().containsMatchIn(param)) throw HexoNotationFormatException("Invalid parameter")

        return parseLink(param)
    }

    abstract suspend fun parseLink(param: String): Board
}
