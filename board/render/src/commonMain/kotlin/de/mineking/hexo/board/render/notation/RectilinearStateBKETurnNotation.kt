@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.board.render.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.board.minus
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.times

object RectilinearStateBKETurnNotationBoardRenderer : BoardRenderer<Unit, String> {
    override suspend fun render(board: Board, param: Unit) = board.renderRectilinearStateBKETurnNotation()
}

fun Board.renderRectilinearStateBKETurnNotation(): String {
    val turns = mutableMapOf<Int, Pair<CellOwner, MutableList<CellCoordinate>>>()
    val board = copy()
    cells.forEach { (coordinate, cell) ->
        val owner = cell.owner ?: return@forEach
        val turn = cell.turn ?: return@forEach
        val cells = turns[turn]

        when {
            cells == null -> turns[turn] = owner to mutableListOf(coordinate)
            cells.first == owner -> cells.second += coordinate
            else -> return@forEach
        }

        board.cells[coordinate]?.owner = null
    }

    val renderedState = board.renderRectilinearNotationInternal(RectilinearNotationType.Compact)
    val state = renderedState.notation
    if (turns.isEmpty()) return state

    val normalizedTurns = turns.mapValues { (_, moves) -> moves.first to moves.second.map { it - renderedState.topLeft } }
    val renderedTurns = normalizedTurns.entries
        .filter { it.key > 0 }
        .sortedBy { it.key }
        .map { it.value }
        .renderBKETurns(origin = normalizedTurns[0]?.second?.firstOrNull(), includePrefix = state.isNotEmpty())

    if (state.isEmpty()) return renderedTurns

    return "$state, $renderedTurns"
}

private fun List<Pair<CellOwner, List<CellCoordinate>>>.renderBKETurns(origin: CellCoordinate?, includePrefix: Boolean) = buildString {
    if (this@renderBKETurns.isEmpty()) {
        append("0")
        return@buildString
    }

    val layout = chooseBKELayout(origin)

    if (includePrefix) {
        append("${layout.baseline.symbol} @(${layout.origin.q}, ${layout.origin.r}) ")
    }

    this@renderBKETurns.forEach { (owner, cells) ->
        append("${owner.symbol} ")
        cells.forEach { cell ->
            val (ring, sector, sectorOffset) = cell.ringOffset(layout.origin, layout.baseline)
            append(ring.toBKERingLabel())

            if (ring == 1) {
                append(sector)
            } else {
                if (sector != 0) append("$sector.")
                append(sectorOffset)
            }

            append(" ")
        }
    }
}.trimEnd()

private data class BKELayout(val origin: CellCoordinate, val baseline: Direction)

private fun List<Pair<CellOwner, List<CellCoordinate>>>.chooseBKELayout(origin: CellCoordinate?): BKELayout {
    val cells = flatMap { it.second }
    val forbiddenOrigins = cells.toSet()

    if (origin != null && origin !in forbiddenOrigins) {
        return Direction.entries
            .map { direction -> BKELayout(origin, direction) }
            .chooseBest(cells)
    }

    val first = cells.first()
    val maxDistance = cells.maxOf { first.distanceTo(it) } + 1
    return (1..maxDistance)
        .flatMap { ring ->
            Direction.entries.map { direction -> BKELayout(first - direction.direction * ring, direction) }
        }
        .filter { it.origin !in forbiddenOrigins }
        .chooseBest(cells)
}

private fun BKELayout.score(cells: List<CellCoordinate>) = cells.flatMap {
    val (ring, sector, sectorOffset) = it.ringOffset(origin, baseline)
    listOf(ring, sector * ring + sectorOffset)
}

private fun List<BKELayout>.chooseBest(cells: List<CellCoordinate>) = minWith { a, b ->
    val scoreCompare = compareScores(a.score(cells), b.score(cells))
    if (scoreCompare != 0) {
        scoreCompare
    } else {
        compareValuesBy(a, b, { it.baseline.ordinal }, { it.origin.q }, { it.origin.r })
    }
}

private fun compareScores(a: List<Int>, b: List<Int>): Int {
    a.zip(b).forEach { (left, right) ->
        left.compareTo(right).takeIf { it != 0 }?.let { return it }
    }

    return a.size.compareTo(b.size)
}

private data class RingOffset(val ring: Int, val sector: Int, val sectorOffset: Int)
private fun CellCoordinate.ringOffset(origin: CellCoordinate, baseline: Direction): RingOffset {
    require(this != origin) { "BKE origin cannot be a rendered move" }

    val relative = this - origin
    val ring = distanceTo(origin)

    var corner = baseline.direction * ring
    for (sector in Direction.entries.indices) {
        val direction = baseline.ringDirection(sector)
        for (i in 0 until ring) {
            if (corner + direction * i == relative) {
                return RingOffset(ring = ring, sector = sector, sectorOffset = i)
            }
        }

        corner += direction * ring
    }

    error("Coordinate $relative is not on ring $ring")
}

private fun Int.toBKERingLabel(): String {
    require(this > 0) { "BKE ring must be positive" }

    var ring = this
    return buildString {
        while (ring > 0) {
            ring--
            append('A' + ring % 26)
            ring /= 26
        }
    }.reversed()
}
