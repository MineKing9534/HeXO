package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationFormatException

abstract class LinkParser(private val prefix: String) : BoardParser {
    final override suspend fun parse(notation: String): Board {
        val trimmedNotation = notation.trim()

        if (!trimmedNotation.startsWith(prefix)) throw HexoNotationFormatException("Invalid link")

        val param = trimmedNotation.substring(startIndex = prefix.length)
        if (""".*\s.*""".toRegex().containsMatchIn(param)) throw HexoNotationFormatException("Invalid parameter")

        return parseLink(param)
    }

    abstract suspend fun parseLink(param: String): Board
}

object TytoLinkParser : LinkParser(prefix = "https://hexo.tyto.cc/analysis#c=") {
    override suspend fun parseLink(param: String): Board {
        return param.parseTytoNotation(focusWinningRows = false)
    }
}
