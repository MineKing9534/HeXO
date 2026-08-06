package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.requireHexo

private val VERSION_PATTERN = """^version\[(\d+)\]$""".toRegex()
private val COORDINATE_PATTERN = """\[\s*(-?\d+)\s*,\s*(-?\d+)\s*\]""".toRegex()
private val TURN_PATTERN = """^(\d+)\.\s*((?:\[\s*-?\d+\s*,\s*-?\d+\s*\]\s*)+)$""".toRegex()

object HTTTXNotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseHTTTXNotation(focusWinningRows = false)
}

fun String.parseHTTTXNotation(focusWinningRows: Boolean = true): Board {
    val statements = split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    requireHexo(statements.isNotEmpty()) { "Cannot parse an empty HTTTX notation" }

    val version = VERSION_PATTERN.matchEntire(statements.first())?.groupValues?.get(1)?.toInt()
        ?: throw HexoNotationFormatException("HTTTX notation has to start with `version[n];`")

    requireHexo(version == 1) { "Unsupported HTTTX notation version `$version`" }
    requireHexo(statements.size > 1) { "HTTTX notation has to contain at least one turn" }

    val turns = statements.drop(1).mapIndexed { index, statement ->
        statement.parseHTTTXTurn(index + 1)
    }

    val board = MutableBoard()
    board[0, 0].apply {
        owner = CellOwner.X
        turn = 0
    }

    turns.forEach { turn ->
        val owner = CellOwner.entries[turn.number % 2]

        turn.moves.forEach { coordinate ->
            requireHexo(coordinate !in board.cells) { "Duplicate HTTTX move at $coordinate" }

            board[coordinate].apply {
                this.owner = owner
                this.turn = turn.number
            }
        }
    }

    return board.apply {
        if (focusWinningRows) focusWinningRows()
    }
}

private data class HTTTXTurn(
    val number: Int,
    val moves: List<CellCoordinate>,
)

private fun String.parseHTTTXTurn(expectedNumber: Int): HTTTXTurn {
    val match = TURN_PATTERN.matchEntire(this)
        ?: throw HexoNotationException("Invalid HTTTX turn `$this`, expected `<number>. [q,r]...`")

    val number = match.groupValues[1].toInt()
    requireHexo(number == expectedNumber) { "Expected HTTTX turn `$expectedNumber` but found `$number`" }

    val coordinates = COORDINATE_PATTERN.findAll(match.groupValues[2]).map {
        val (q, r) = it.groupValues.drop(1).map(String::toInt)
        CellCoordinate(
            q = q + r,
            r = -r,
        )
    }.toList()

    requireHexo(coordinates.isNotEmpty()) { "HTTTX turn `$number` has to contain at least one move" }

    return HTTTXTurn(number, coordinates)
}
