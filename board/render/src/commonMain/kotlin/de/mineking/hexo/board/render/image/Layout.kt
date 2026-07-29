package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.board.endInclusive
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal const val SQRT3 = 1.7320508075688772

enum class BoardRenderBounds {
    Compact,
    IncludeSurroundings,
}

data class BoundingBox(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double,
)

fun BoundingBox.pad(padding: Int) = copy(minX = minX - padding, maxX = maxX + padding, minY = minY - padding, maxY = maxY + padding)

operator fun BoundingBox.contains(point: Point) = point.x in minX..maxX && point.y in minY..maxY

val BoundingBox.width get() = ceil(maxX - minX).toInt()
val BoundingBox.height get() = ceil(maxY - minY).toInt()

val BoundingBox.topLeft get() = Point(minX, minY)
val BoundingBox.bottomRight get() = Point(maxX, maxY)

val BoundingBox.center get() = Point(
    (maxX + minX) / 2,
    (maxY + minY) / 2,
)

data class RenderSize(val layoutRadius: Double) {
    fun CellCoordinate.toPixel(): Point {
        val x = layoutRadius * (SQRT3 * q + SQRT3 / 2 * r)
        val y = layoutRadius * (3.0 / 2 * r)
        return Point(x, y)
    }
}

data class BoardRenderLayout(
    val size: RenderSize,
    val boundingBox: BoundingBox,
    val coordinates: Set<CellCoordinate>,
    val board: Board,
) {
    fun Point.toCoordinate(): CellCoordinate {
        val r = y / (size.layoutRadius * 1.5)
        val q = x / (size.layoutRadius * SQRT3) - r / 2.0

        return roundAxial(q, r)
    }

    private fun roundAxial(q: Double, r: Double): CellCoordinate {
        var roundedQ = q.roundToInt()
        val roundedS = (-q - r).roundToInt()
        var roundedR = r.roundToInt()

        val qDiff = abs(roundedQ - q)
        val sDiff = abs(roundedS - (-q - r))
        val rDiff = abs(roundedR - r)

        when {
            qDiff > sDiff && qDiff > rDiff -> roundedQ = -roundedS - roundedR
            rDiff > sDiff -> roundedR = -roundedQ - roundedS
        }

        return CellCoordinate(roundedQ, roundedR)
    }
}

const val DEFAULT_VISIBLE_RADIUS = 8

fun Board.createRenderLayout(
    layoutRadius: Double,
    bounds: BoardRenderBounds,
    visibleRadius: Int,
): BoardRenderLayout {
    val visibleCoordinates = findVisibleCoordinates(visibleRadius)
    val size = RenderSize(layoutRadius)
    return BoardRenderLayout(
        size = size,
        boundingBox = findBoundingBox(size, when (bounds) {
            BoardRenderBounds.IncludeSurroundings -> visibleCoordinates
            BoardRenderBounds.Compact -> cells.keys.ifEmpty { setOf(CellCoordinate.Zero) }
        }),
        coordinates = visibleCoordinates,
        board = this,
    )
}

private fun Board.findBoundingBox(size: RenderSize, visibleCoordinates: Set<CellCoordinate>): BoundingBox {
    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY

    val endPoints = lineHighlights.flatMap { listOf(it.start, it.endInclusive) }
    val positions = visibleCoordinates + endPoints
    for (position in positions.ifEmpty { listOf(CellCoordinate.Zero) }) {
        val center = size.run { position.toPixel() }
        val hex = center.createHex(size.layoutRadius)

        hex.points.forEach { (x, y) ->
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }
    }

    return BoundingBox(
        minX = minX,
        maxX = maxX,
        minY = minY,
        maxY = maxY,
    )
}

internal fun Board.findVisibleCoordinates(distance: Int): Set<CellCoordinate> {
    val occupied = cells
        .filterValues { it.owner != null || it.highlight != null || it.focused || it.label.isNotBlank() }
        .keys
        .ifEmpty { setOf(CellCoordinate.Zero) }

    return buildSet {
        for (origin in occupied) {
            for (dq in -distance + 1 until distance) {
                for (dr in -distance + 1 until distance) {
                    val coordinate = CellCoordinate(origin.q + dq, origin.r + dr)
                    if (origin.distanceTo(coordinate) < distance) {
                        add(coordinate)
                    }
                }
            }
        }
    }
}
