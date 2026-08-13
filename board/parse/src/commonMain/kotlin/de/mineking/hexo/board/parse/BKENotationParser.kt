package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.requireHexo
import de.mineking.hexo.board.times

private const val MOVE_PATTERN = /*language=regexp*/ """[A-Z]+(?:[0-5]\.)?\d+"""
private const val TURN_PATTERN = /*language=regexp*/ """[xo](?:\s+$MOVE_PATTERN)+"""
private const val TURN_LIST_PATTERN = /*language=regexp*/ """$TURN_PATTERN(?:\s+$TURN_PATTERN)*"""

private const val ORIGIN_PATTERN = /*language=regexp*/ """@\s*\((-?\d+),\s*(-?\d+)\)"""

private val BKE_FORMAT_CHECK_PATTERN = ".*$TURN_PATTERN.*".toRegex()
private val EXTENDED_BKE_PATTERN = """^\s*([bdpq<>])?\s*(CW|CCW)?\s*(?:$ORIGIN_PATTERN\s*:?)?\s*($TURN_LIST_PATTERN)\s*$""".toRegex()

enum class Chirality(val symbol: String) {
    Clockwise("CW"),
    CounterClockwise("CCW"),
    ;

    companion object {
        fun fromSymbol(symbol: String) = entries.firstOrNull { it.symbol == symbol }
            ?: throw HexoNotationException("Unknown symbol: $symbol. Valid symbols are ${entries.joinToString { "`${it.symbol}`" }}")
    }
}

object BKENotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation
        .parseExtendedBKENotation(implicitOrigin = true, focusWinningRows = false)
}

fun String.parseBKENotation(
    origin: CellCoordinate?,
    zeroOffsetLine: Direction,
    chirality: Chirality = Chirality.Clockwise,
    focusWinningRows: Boolean = true,
): Board {
    bkePrecheck()?.let { return it }

    val turns = parseBKETurns()
    val board = MutableBoard()

    val origin = origin ?: run {
        board[0, 0].apply {
            owner = CellOwner.X
            turn = 0
        }
        CellCoordinate.Zero
    }

    turns.forEachIndexed { index, (player, moves) ->
        moves.forEach {
            val coordinate = it.toCellCoordinate(origin, zeroOffsetLine, chirality)
            requireHexo(coordinate !in board.cells) { "Duplicate BKE move at $this" }

            board[coordinate].apply {
                owner = player
                turn = index + 1
            }
        }
    }

    return board.apply {
        if (focusWinningRows) focusWinningRows()
        attributes[BoardAttribute.ShowTurnNumbers] = true
    }
}

fun String.parseExtendedBKENotation(
    implicitOrigin: Boolean,
    focusWinningRows: Boolean = true,
): Board {
    bkePrecheck()?.let { return it }
    val match = EXTENDED_BKE_PATTERN.matchEntire(this) ?: throw HexoNotationException("Invalid extended BKE notation, use `[b,d,p,q,<,>][CW,CCW]?")

    val (_, zeroOffsetLineSymbol, chiralitySymbol, originQ, originR, content) = match.groupValues
    val zeroOffsetLine = if (zeroOffsetLineSymbol.isNotEmpty()) Direction.fromSymbol(zeroOffsetLineSymbol) else Direction.TopRight
    val chirality = if (chiralitySymbol.isNotEmpty()) Chirality.fromSymbol(chiralitySymbol) else Chirality.Clockwise
    val origin = if (implicitOrigin) {
        requireHexo(originR.isEmpty()) { "Origin cannot be specified in BKE with implicit origin" }
        null
    } else {
        if (originR.isNotEmpty()) CellCoordinate(originQ.toInt(), originR.toInt()) else CellCoordinate.Zero
    }

    return content.parseBKENotation(origin, zeroOffsetLine, chirality, focusWinningRows)
}

private fun String.bkePrecheck(): Board? {
    if (trim() == "0") {
        return MutableBoard().apply {
            this[0, 0].apply {
                owner = CellOwner.X
                turn = 0
            }
        }
    }

    requireHexo(matches(BKE_FORMAT_CHECK_PATTERN), notationCheck = true) { "Invalid bke notation" }
    return null
}

private operator fun <T> List<T>.component6() = get(5)

private data class RingOffset(
    val ring: Int,
    val offset: Int,
)

private data class Turn(
    val player: CellOwner,
    val moves: List<RingOffset>,
)

private fun String.parseBKETurns(): List<Turn> {
    val parts = trim().split(Regex("\\s+"))
    requireHexo(parts.isNotEmpty()) {
        "BKE turns have to be space separated blocks of the format `[x|o] [A-Z]<offset>...`"
    }

    val turns = mutableListOf<Turn>()
    var offset = 0
    while (offset < parts.size) {
        val player = parts[offset].parsePlayer()
        offset++

        val moves = mutableListOf<RingOffset>()
        while (offset < parts.size && !parts[offset].isPlayer()) {
            moves += parts[offset].parseRingOffset()
            offset++
        }

        requireHexo(moves.isNotEmpty()) {
            "BKE turns have to contain at least one move"
        }
        turns += Turn(player, moves)
    }

    return turns
}

private fun String.isPlayer() = this == "o" || this == "x"

private fun String.parsePlayer() = when (this) {
    "o" -> CellOwner.O
    "x" -> CellOwner.X
    else -> throw HexoNotationException("Invalid player `$this`")
}

private fun String.parseRingOffset(): RingOffset {
    val ringLabel = takeWhile { it in 'A'..'Z' }
    requireHexo(ringLabel.isNotEmpty() && ringLabel.length < length) {
        "Invalid bke move `$this`, has to be in format `[A-Z]+<offset>`"
    }
    val ring = try {
        ringLabel.toBKERing()
    } catch (_: IllegalArgumentException) {
        throw HexoNotationException("Invalid ring `$ringLabel`")
    }

    val offset = drop(ringLabel.length).let {
        val parts = it.split(".")
        if (parts.size == 1) {
            it.toInt()
        } else {
            requireHexo(parts.size == 2) { "Invalid bke move `$this`, has to be in format `[A-Z]+<offset>`" }
            val (sector, offset) = parts
            sector.toInt() * ring + offset.toInt()
        }
    }
    requireHexo(offset >= 0 && offset < 6 * ring) { "Invalid offset $offset for ring $ring. Offset has to be be in 0..<${6 * ring}" }

    return RingOffset(ring, offset)
}

private fun RingOffset.toCellCoordinate(origin: CellCoordinate, zeroOffsetLine: Direction, chirality: Chirality): CellCoordinate {
    val perimeter = 6 * ring
    val orientedOffset = if (chirality == Chirality.CounterClockwise) (perimeter - offset) % perimeter else offset

    val sector = orientedOffset / ring
    val offset = orientedOffset % ring

    val fullSides = (0 until sector).fold(CellCoordinate.Zero) { acc, i -> acc + zeroOffsetLine.ringDirection(i) * ring }
    return origin + zeroOffsetLine.direction * ring + fullSides + zeroOffsetLine.ringDirection(sector) * offset
}

private fun String.toBKERing(): Int {
    require(isNotEmpty() && all { it in 'A'..'Z' }) { "Invalid BKE ring label `$this`" }

    var ring = 0L
    for (character in this) {
        ring = ring * 26 + (character - 'A' + 1)
        require(ring <= Int.MAX_VALUE) { "BKE ring label `$this` is too large" }
    }
    return ring.toInt()
}
