package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.MutableCell
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.parse.generated.HTTTXLexer
import de.mineking.hexo.board.parse.generated.HTTTXParser
import de.mineking.hexo.board.requireHexo

object HTTTXNotationParser : NotationParser {
    override suspend fun parse(notation: String) = notation.parseHTTTXNotation()
}

fun String.parseHTTTXNotation(): Board {
    val root = parseInternal(this)

    val versionText = root.INTEGER().text
    val version = versionText.toIntOrNull() ?: throw HexoNotationException("HTTTX notation version `$versionText` is too large")
    requireHexo(version == 1 || version == 2) {
        "Unsupported HTTTX notation version `$version`"
    }

    val board = MutableBoard()
    board[CellCoordinate.Zero] = MutableCell(CellOwner.X, turn = 0)

    root.turn().forEachIndexed { index, turn ->
        val expectedNumber = index + 1
        val numberText = turn.INTEGER().text
        val number = numberText.toIntOrNull()
            ?: throw HexoNotationException("HTTTX turn number `$numberText` is too large")

        requireHexo(number == expectedNumber) {
            "Expected HTTTX turn `$expectedNumber` but found `$number`"
        }

        val owner = if (number % 2 == 0) CellOwner.X else CellOwner.O
        turn.move().forEach { move ->
            if (version < 2) {
                requireHexo(move.visual().isEmpty()) { "Visuals are only supported in HTTTX version 2 an onward" }
            }

            val coordinate = move.coordinate().toCellCoordinate(number)

            requireHexo(coordinate !in board.cells) {
                "Duplicate HTTTX move at $coordinate"
            }
            board[coordinate] = MutableCell(owner, turn = number)
        }
    }

    val lastTurn = root.turn().last()
    lastTurn.move().last().visual().forEach { visual ->
        val coordinate = visual.coordinate().toCellCoordinate(root.turn().size)
        val highlight = visual.HIGHLIGHT()
        val label = visual.LABEL()

        board[coordinate].apply {
            if (highlight != null) {
                val symbol = highlight.text
                    .substring(1)
                    .takeIf { it.isNotBlank() }

                this.highlight = CellHighlight(symbol?.let { CellOwner.valueOf(it.uppercase()) })
            }

            if (label != null) {
                this.label = label.text.substring(1)
            }
        }
    }

    return board
}

private fun parseInternal(input: String) = parseANTLRNotation(input, ::HTTTXLexer, ::HTTTXParser, HTTTXParser::root)

private fun HTTTXParser.CoordinateContext.toCellCoordinate(turn: Int): CellCoordinate {
    val (q, r) = INTEGER().map { token ->
        token.text.toIntOrNull()
            ?: throw HexoNotationException("Coordinate `${token.text}` in HTTTX turn `$turn` is too large")
    }
    val convertedQ = q.toLong() + r
    val convertedR = -r.toLong()

    val supportedRange = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
    requireHexo(convertedQ in supportedRange && convertedR in supportedRange) {
        "Coordinate `[$q,$r]` in HTTTX turn `$turn` is outside the supported range"
    }

    return CellCoordinate(convertedQ.toInt(), convertedR.toInt())
}
