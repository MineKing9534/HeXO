package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.MutableCell
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.parse.generated.BKELexer
import de.mineking.hexo.board.parse.generated.BKEParser
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.requireHexo
import de.mineking.hexo.board.times
import de.mineking.hexo.board.to
import de.mineking.hexo.board.withAttributes
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream

object BKENotationParser : NotationParser {
    override suspend fun parse(notation: String) = notation.parseBKENotation()
}

fun String.parseBKENotation(): Board {
    val root = parseInternal(this)
    val board = MutableBoard().withAttributes(BoardAttribute.ShowTurnNumbers to true)
    if (root.turn().isEmpty()) {
        board[CellCoordinate.Zero] = MutableCell(CellOwner.X, turn = 0)
        return board
    }

    val turns = root.turn().map { turn ->
        ParsedTurn(
            player = when (turn.player().text) {
                "x" -> CellOwner.X
                "o" -> CellOwner.O
                else -> error("ANTLR accepted an unknown BKE player")
            },
            moves = turn.MOVE().map { it.text.parseRingOffset() },
        )
    }

    val context = root.context()
    val (zeroOffsetLine, chirality) = if (context == null) {
        board[CellCoordinate.Zero] = MutableCell(turns.first().player.other, turn = 0)
        Direction.TopRight to 1
    } else {
        val zeroOffsetLine = Direction.fromSymbol(context.DIRECTION().text)
        val chirality = if (context.CHIRALITY().text == "CW") 1 else -1

        zeroOffsetLine to chirality
    }

    turns.forEachIndexed { index, turn ->
        requireHexo(index == 0 || turn.player != turns[index - 1].player) {
            "Consecutive BKE turns cannot use the same player `${turn.player.symbol}`"
        }
        turn.moves.forEach { move ->
            val coordinate = move.toCellCoordinate(zeroOffsetLine, chirality)
            requireHexo(coordinate !in board.cells) {
                "Duplicate BKE move `${move.source}` at $coordinate"
            }
            board[coordinate] = MutableCell(
                owner = turn.player,
                turn = index + 1,
            )
        }
    }

    return board
}

private fun parseInternal(input: String): BKEParser.RootContext {
    val errorListener = CustomErrorListener()

    val lexer = BKELexer(CharStreams.fromString(input)).apply {
        removeErrorListeners()
        addErrorListener(errorListener)
    }
    val parser = BKEParser(CommonTokenStream(lexer)).apply {
        removeErrorListeners()
        addErrorListener(errorListener)
    }

    return parser.root()
}

private data class ParsedTurn(
    val player: CellOwner,
    val moves: List<RingOffset>,
)

private data class RingOffset(
    val source: String,
    val ring: Int,
    val offset: Int,
)

private fun String.parseRingOffset(): RingOffset {
    val ringLabel = takeWhile { it in 'A'..'Z' }
    requireHexo(ringLabel.isNotEmpty()) { "Move `$this` has no ring" }

    val ring = ringLabel.toBKERing()

    val offsetParts = drop(ringLabel.length).split('.')
    requireHexo(offsetParts.size in 1..2 && offsetParts.all { it.isNotEmpty() }) {
        "Move `$this` must use `<ring><offset>` or `<ring><sector>.<offset>`"
    }

    val offsets = offsetParts.map {
        it.toIntOrNull()
            ?: throw HexoNotationException("Number `$it` in move `$this` is too large")
    }

    val offset = if (offsets.size == 1) {
        offsets.single().toLong()
    } else {
        val (sector, sectorOffset) = offsets
        requireHexo(sector in 0..5) {
            "Invalid sector $sector in move `$this`; expected 0..5"
        }
        requireHexo(sectorOffset in 0 until ring) {
            "Invalid sector offset $sectorOffset in move `$this`; expected 0..<$ring"
        }
        sector.toLong() * ring + sectorOffset
    }

    val perimeter = 6L * ring
    requireHexo(offset in 0 until perimeter) {
        "Invalid offset $offset for ring $ring in move `$this`; expected 0..<$perimeter"
    }

    return RingOffset(this, ring, offset.toInt())
}

private fun RingOffset.toCellCoordinate(zeroOffsetLine: Direction, chirality: Int): CellCoordinate {
    val clockwiseOffset = if (chirality == 1) offset.toLong() else (6L * ring - offset) % (6L * ring)
    val sector = (clockwiseOffset / ring).toInt()
    val sectorOffset = (clockwiseOffset % ring).toInt()
    val fullSides = (0 until sector).fold(CellCoordinate.Zero) { coordinate, index ->
        coordinate + zeroOffsetLine.ringDirection(index) * ring
    }

    return CellCoordinate.Zero +
        zeroOffsetLine.direction * ring +
        fullSides + zeroOffsetLine.ringDirection(sector) * sectorOffset
}

private fun String.toBKERing(): Int {
    var ring = 0L
    for (character in this) {
        ring = ring * 26 + (character - 'A' + 1)
        requireHexo(ring <= Int.MAX_VALUE) { "BKE ring label `$this` is too large" }
    }
    return ring.toInt()
}
