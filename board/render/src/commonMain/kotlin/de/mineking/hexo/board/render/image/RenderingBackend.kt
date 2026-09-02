package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import kotlin.math.hypot
import kotlin.math.min

data class Point(val x: Double, val y: Double) {
    constructor(x: Number, y: Number) : this(x.toDouble(), y.toDouble())

    companion object {
        val Zero = Point(0.0, 0.0)
    }
}

data class Polygon(val points: List<Point>)

data class PolygonPath(val start: Point, val segments: List<Segment>) {
    sealed interface Segment {
        data class Line(val to: Point) : Segment
        data class QuadraticCurve(val control: Point, val to: Point) : Segment
    }
}

data class Stroke(val color: Color, val width: Float)

interface RenderingBackend {
    fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke? = null)
    fun drawPolygon(shape: Polygon, color: Color, outline: Stroke? = null, borderRadius: Float = 0f)
    fun drawString(point: Point, text: String, maxWidth: Double, fontSize: Float, font: FontType, color: Color)
}

fun RenderingBackend.drawCircle(
    point: Point,
    stroke: Stroke,
    outline: Stroke? = null,
) {
    drawLine(from = point, to = point, stroke = stroke, outline = outline)
}

operator fun Point.plus(point: Point) = Point(x + point.x, y + point.y)
operator fun Point.minus(point: Point) = Point(x - point.x, y - point.y)
operator fun Point.times(value: Double) = Point(x * value, y * value)
operator fun Point.div(value: Double) = Point(x / value, y / value)

fun Polygon.toPath(borderRadius: Float = 0f): PolygonPath {
    if (points.isEmpty()) return PolygonPath(Point.Zero, emptyList())

    val radius = borderRadius.toDouble()
    if (radius <= 0.0 || points.size < 3) {
        return PolygonPath(
            start = points.first(),
            segments = points.drop(1).map { PolygonPath.Segment.Line(it) },
        )
    }

    fun Point.distanceTo(other: Point) = hypot(x - other.x, y - other.y)
    fun Point.pointTowards(other: Point, distance: Double): Point {
        val totalDistance = distanceTo(other)
        if (totalDistance == 0.0) return this

        return this + (other - this) * (distance / totalDistance)
    }

    fun corner(index: Int): Pair<Point, Point> {
        val point = points[index]
        val previous = points[(index - 1 + points.size) % points.size]
        val next = points[(index + 1) % points.size]
        val cornerRadius = min(radius, min(point.distanceTo(previous), point.distanceTo(next)) / 2.0)

        return point.pointTowards(previous, cornerRadius) to point.pointTowards(next, cornerRadius)
    }

    val (_, start) = corner(0)
    val segments = ((1 until points.size) + 0).flatMap { index ->
        val point = points[index]
        val (cornerStart, cornerEnd) = corner(index)

        listOf(
            PolygonPath.Segment.Line(cornerStart),
            PolygonPath.Segment.QuadraticCurve(point, cornerEnd),
        )
    }

    return PolygonPath(start, segments)
}
