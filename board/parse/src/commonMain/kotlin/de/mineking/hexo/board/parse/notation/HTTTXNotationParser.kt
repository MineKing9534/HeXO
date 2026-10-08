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

    val version = root.metadata().version().validate()

    val (board, nextPlayer) = createBoard(version, root.setup())

    val turns = root.turn()
    board.addTurns(version, nextPlayer, turns)
    turns.lastOrNull()?.let { turn ->
        board.addVisuals("turn ${turns.size}", turn.move().last().visual())
    }

    return board
}

private fun createBoard(version: HTTTXVersion, setup: HTTTXParser.SetupContext?): Pair<MutableBoard, CellOwner> {
    val board = MutableBoard()

    if (setup == null) {
        board[CellCoordinate.Zero] = MutableCell(CellOwner.X, turn = 0)
        return board to CellOwner.O
    }

    requireHexo(version.version >= 2) {
        "Setup is only supported in HTTTX version 2 and onward"
    }

    fun addState(owner: CellOwner, moves: List<HTTTXParser.CoordinateContext>) {
        moves.forEach {
            val coordinate = it.toCellCoordinate("setup $owner")
            requireHexo(coordinate !in board.cells) {
                "Duplicate HTTTX setup cell at $coordinate"
            }
            board[coordinate].owner = owner
        }
    }

    fun player(index: Int) = CellOwner.valueOf(setup.player_setup()[index].PLAYER().text.uppercase()) to setup.player_setup()[index].coordinate()

    val (firstPlayer, firstMoves) = player(0)
    val (secondPlayer, secondMoves) = player(1)

    addState(firstPlayer, firstMoves)
    addState(secondPlayer, secondMoves)

    requireHexo(firstPlayer != secondPlayer) {
        "HTTTX setup cannot define the same player twice"
    }

    board.addVisuals("setup", setup.visual())

    return board to firstPlayer
}

private fun MutableBoard.addTurns(
    version: HTTTXVersion,
    firstPlayer: CellOwner,
    turns: List<HTTTXParser.TurnContext>,
) {
    turns.forEachIndexed { index, turn ->
        val expectedNumber = index + 1
        val numberText = turn.INTEGER().text
        val number = numberText.toIntOrNull()
            ?: throw HexoNotationException("HTTTX turn number `$numberText` is too large")

        requireHexo(number == expectedNumber) {
            "Expected HTTTX turn `$expectedNumber` but found `$number`"
        }

        val owner = CellOwner.entries[(index + firstPlayer.ordinal) % 2]
        turn.move().forEach { move ->
            if (version.version < 2) {
                requireHexo(move.visual().isEmpty()) { "Visuals are only supported in HTTTX version 2 an onward" }
            }

            val coordinate = move.coordinate()?.toCellCoordinate("turn $number")
            if (coordinate == null) {
                requireHexo(version.version >= 2) { "Skipped moves are only supported in HTTTX version 2 and onward" }
                return@forEach
            }

            requireHexo(coordinate !in cells) {
                "Duplicate HTTTX move at $coordinate"
            }
            this[coordinate] = MutableCell(owner, turn = number)
        }
    }
}

private fun MutableBoard.addVisuals(turn: String, visuals: List<HTTTXParser.VisualContext>) {
    visuals.forEach { visual ->
        val coordinate = visual.coordinate().toCellCoordinate(turn)
        val highlight = visual.highlight()
        val label = visual.label()

        this[coordinate].apply {
            if (highlight != null) {
                val symbol = highlight.text.takeIf { it.isNotBlank() && it.lowercase() != "n" }
                this.highlight = CellHighlight(symbol?.let { CellOwner.valueOf(it.uppercase()) })
            }

            if (label != null) {
                this.label = label.text.unescape()
            }
        }
    }
}

private data class HTTTXVersion(val version: Int, val extensions: Set<HTTTXExtension>)

private fun HTTTXParser.VersionContext.validate(): HTTTXVersion {
    // Whitespace is skipped by the lexer, so compare the source span with the parsed text.
    requireHexo(stop!!.stopIndex - start!!.startIndex + 1 == text.length) {
        "Whitespace is not allowed in HTTTX version tags"
    }

    val extensions = extension().flatMap { it.text.toList() }
    val version = INTEGER().text.toIntOrNull()
        ?: throw HexoNotationException("HTTTX notation version `${INTEGER().text}` is too large")

    requireHexo(version == 1 || version == 2) {
        "Unsupported HTTTX notation version `$version`"
    }

    requireHexo(version >= 2 || extensions.isEmpty()) {
        "Extensions are only supported in HTTTX version 2 and onward"
    }

    return HTTTXVersion(
        version = version,
        extensions = HTTTXExtension.of(extensions),
    )
}

private enum class HTTTXExtension(val symbol: Char) {
    Unlimited('u'),
    ;

    companion object {
        fun of(symbols: List<Char>) = buildSet {
            symbols.forEach { symbol ->
                val extension = HTTTXExtension.entries.firstOrNull { it.symbol == symbol }
                    ?: throw HexoNotationException("Unknown HTTTX extension '$symbol'")

                requireHexo(add(extension)) {
                    "Duplicate HTTTX extension '$symbol'"
                }
            }
        }
    }
}

private fun parseInternal(input: String) = parseANTLRNotation(input, ::HTTTXLexer, ::HTTTXParser, HTTTXParser::root)

private fun HTTTXParser.CoordinateContext.toCellCoordinate(turn: String): CellCoordinate {
    val (q, r) = INTEGER().map { token ->
        token.text.toIntOrNull()
            ?: throw HexoNotationException("Coordinate `${token.text}` in HTTTX `$turn` is too large")
    }
    val convertedQ = q.toLong() + r
    val convertedR = -r.toLong()

    val supportedRange = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
    requireHexo(convertedQ in supportedRange && convertedR in supportedRange) {
        "Coordinate `[$q,$r]` in HTTTX turn `$turn` is outside the supported range"
    }

    return CellCoordinate(convertedQ.toInt(), convertedR.toInt())
}
