@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.board.render.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.GamePosition
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.board.minus
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.times
import de.mineking.hexo.board.toGamePositionForce

object BKENotationBoardRenderer : BoardRenderer<Unit, String> {
    override suspend fun render(board: Board, param: Unit) = board
        .toGamePositionForce()
        .renderBKENotation()
}

fun GamePosition<*>.renderBKENotation(origin: CellCoordinate? = null): String {
    val ringOrigin = origin ?: run {
        require(turns.firstOrNull()?.moves?.size == 1) {
            "BKE notation without an explicit origin requires a single opening move"
        }
        turns.first().moves.single().coordinate
    }

    val renderedTurns = if (origin == null) turns.drop(1) else turns
    if (origin == null && renderedTurns.isEmpty()) return "0"
    if (renderedTurns.isEmpty()) return ""

    val ringOffsets = renderedTurns.flatMap { turn ->
        turn.moves.map { move -> (move.coordinate - ringOrigin).toBKERingOffset() }
    }

    val openingMoves = if (origin == null) 0 else turns.first().moves.size
    val comparisonOffsets = ringOffsets.drop(openingMoves) + ringOffsets.take(openingMoves)
    val orientations = (listOf(Direction.TopRight) + Direction.entries.filter { it != Direction.TopRight })
        .flatMap { direction -> listOf(BKEOrientation(direction, 1), BKEOrientation(direction, -1)) }
    val orientation = orientations.minWith { left, right ->
        comparisonOffsets.firstNotNullOfOrNull { move ->
            left.offset(move).compareTo(right.offset(move)).takeIf { it != 0 }
        } ?: 0
    }

    return buildString {
        if (origin != null) {
            append(orientation.direction.symbol)
            append(if (orientation.chirality == 1) " CW " else " CCW ")
        }
        var index = 0
        renderedTurns.forEachIndexed { turnIndex, turn ->
            if (turnIndex > 0) append(' ')
            append(turn.meta.player.symbol)
            turn.moves.forEach { _ ->
                val move = ringOffsets[index++]
                append(' ')
                append(move.ring.toBKERingLabel())
                append(orientation.offset(move))
            }
        }
    }
}

private data class BKERingOffset(val ring: Int, val offset: Long)

private data class BKEOrientation(val direction: Direction, val chirality: Int) {
    fun offset(move: BKERingOffset): Long {
        val perimeter = 6L * move.ring
        val offset = (move.offset - direction.ordinal.toLong() * move.ring) * chirality
        return (offset % perimeter + perimeter) % perimeter
    }
}

private fun CellCoordinate.toBKERingOffset(): BKERingOffset {
    val ring = distanceTo(CellCoordinate.Zero)
    require(ring > 0) { "A BKE move cannot occupy the explicit origin" }

    for (sector in 0 until 6) {
        val corner = Direction.entries[sector].direction * ring
        val side = Direction.Right.ringDirection(sector)
        val remainder = this - corner
        val offset = if (side.q != 0) remainder.q / side.q else remainder.r / side.r
        if (offset in 0 until ring && side * offset == remainder) {
            return BKERingOffset(ring, sector.toLong() * ring + offset)
        }
    }
    error("Coordinate $this is not on BKE ring $ring")
}

private fun Int.toBKERingLabel(): String {
    var ring = this
    return buildString {
        while (ring > 0) {
            ring--
            append('A' + ring % 26)
            ring /= 26
        }
    }.reversed()
}
