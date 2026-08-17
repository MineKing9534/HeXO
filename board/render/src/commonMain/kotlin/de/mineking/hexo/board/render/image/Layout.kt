package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.board.endInclusive
import de.mineking.hexo.board.isEmpty
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
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
    val size = RenderSize(layoutRadius)
    val compactCoordinates = cells.keys.ifEmpty { setOf(CellCoordinate.Zero) }
    val compactBoundingBox = findBoundingBox(size, compactCoordinates)
    val visibleCoordinates = findVisibleCoordinates(
        distance = visibleRadius,
        size = size,
        boundingBox = compactBoundingBox.takeIf { bounds == BoardRenderBounds.Compact },
    )
    return BoardRenderLayout(
        size = size,
        boundingBox = findBoundingBox(size, when (bounds) {
            BoardRenderBounds.IncludeSurroundings -> visibleCoordinates
            BoardRenderBounds.Compact -> compactCoordinates
        }),
        coordinates = visibleCoordinates,
        board = this,
    )
}

private fun Board.renderOrigins() = cells
    .filterValues { !it.isEmpty(includeHighlights = true) }
    .keys
    .ifEmpty { setOf(CellCoordinate.Zero) }

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

private fun Board.findVisibleCoordinates(
    distance: Int,
    size: RenderSize,
    boundingBox: BoundingBox?,
): Set<CellCoordinate> {
    require(distance >= 0) { "Visible radius must not be negative" }
    val occupied = renderOrigins()

    val qRange: IntRange
    val rRange: IntRange
    if (boundingBox == null) {
        val minQ = occupied.minOf { it.q }.toLong() - distance
        val maxQ = occupied.maxOf { it.q }.toLong() + distance
        val minR = occupied.minOf { it.r }.toLong() - distance
        val maxR = occupied.maxOf { it.r }.toLong() + distance

        qRange = IntRange(
            start = minQ.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt(),
            endInclusive = maxQ.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt(),
        )
        rRange = IntRange(
            start = minR.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt(),
            endInclusive = maxR.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt(),
        )
    } else {
        val minR = floor((boundingBox.minY - size.layoutRadius) / (size.layoutRadius * 1.5)).toInt()
        val maxR = ceil((boundingBox.maxY + size.layoutRadius) / (size.layoutRadius * 1.5)).toInt()

        val qValues = listOf(minR, maxR).flatMap { r ->
            listOf(boundingBox.minX - size.layoutRadius, boundingBox.maxX + size.layoutRadius).map { x ->
                x / (size.layoutRadius * SQRT3) - r / 2.0
            }
        }

        qRange = floor(qValues.min()).toInt()..ceil(qValues.max()).toInt()
        rRange = minR..maxR
    }

    return buildSet {
        for (origin in occupied) {
            val originQRange = intersect(qRange, origin.q, distance)
            val originRRange = intersect(rRange, origin.r, distance)

            for (q in originQRange) {
                for (r in originRRange) {
                    val coordinate = CellCoordinate(q, r)
                    if (origin.distanceTo(coordinate) <= distance) {
                        add(coordinate)
                    }
                }
            }
        }
    }
}

private fun intersect(range: IntRange, center: Int, radius: Int): IntRange {
    val start = maxOf(range.first.toLong(), center.toLong() - radius)
    val endInclusive = minOf(range.last.toLong(), center.toLong() + radius)
    if (start > endInclusive) return IntRange.EMPTY

    return start.toInt()..endInclusive.toInt()
}
