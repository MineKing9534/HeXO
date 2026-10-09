package de.mineking.hexo.board.render.image.theme

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.board.endInclusive
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.SQRT3
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createHex
import de.mineking.hexo.board.render.image.drawCircle
import kotlin.math.roundToInt

data class CixTheme(
    override val gap: Double,
    val borderThickness: Double,
    val lineThickness: Double,
    override val backgroundColor: Color,
    val boardBorderColor: Color,
    val nearestEmptyCellColor: Color,
    val furthestEmptyCellColor: Color,
    val emptyCellGradientRadius: Double,
    val occupiedCellScale: Double,
    val emptyCellLabelColor: Color,
    val highlightColor: Color,
    val focusColor: Color,
    override val playerXColor: Color,
    override val playerOColor: Color,
) : BaseTheme() {
    companion object {
        val Default = CixTheme(
            gap = 8.0,
            borderThickness = 1.5,
            lineThickness = 16.0,
            backgroundColor = Color.rgb(0x03060b),
            boardBorderColor = Color.rgb(0x3c4d6c),
            nearestEmptyCellColor = Color.rgb(0x1e2737),
            furthestEmptyCellColor = Color.rgb(0x0f131c),
            emptyCellGradientRadius = 5.0,
            occupiedCellScale = 0.9,
            emptyCellLabelColor = Color.rgb(0xf8fafc),
            highlightColor = Color.rgb(0xf472b6),
            focusColor = Color.rgb(0xf8fafc),
            playerXColor = Color.rgb(0xf6ce3c),
            playerOColor = Color.rgb(0x3fb7f3),
        )
    }

    override fun renderer(context: RenderingContext) = CixRenderer(context, this)

    override fun render(context: RenderingContext, middleLayer: () -> Unit) {
        val renderer = renderer(context)
        renderer.render(context) {
            middleLayer()
            renderer.drawVisibleBorder()
        }
    }

    fun CellOwner?.color(default: Color) = when (this) {
        CellOwner.X -> playerXColor
        CellOwner.O -> playerOColor
        null -> default
    }
}

class CixRenderer(
    context: RenderingContext,
    private val theme: CixTheme,
) : BaseTheme.Renderer(context) {
    private val borderThickness = context.run { theme.borderThickness.relativeWidth() }
    private val lineThickness = context.run { theme.lineThickness.relativeWidth() }
    private val occupiedCoordinates = context.layout.board.cells
        .filterValues { cell -> cell.owner != null }
        .keys

    fun drawVisibleBorder(): Unit = context.run {
        visibleCoordinates.forEach { coordinate ->
            val hex = coordinate.toPixel().createHex(layout.size.layoutRadius)
            Direction.entries.forEachIndexed { index, direction ->
                if (coordinate + direction.direction in layout.coordinates) return@forEachIndexed

                backend.drawLine(
                    from = hex.points[index],
                    to = hex.points[(index + 1) % hex.points.size],
                    stroke = Stroke(theme.boardBorderColor, borderThickness * 3),
                )
            }
        }
    }

    override fun drawCell(point: Point, hex: Polygon, cell: Cell): Unit = context.run {
        val backgroundColor = theme.emptyCellColor(point)
        backend.drawPolygon(
            shape = hex,
            color = backgroundColor,
        )

        val cellColor = theme.run { cell.owner.color(backgroundColor) }
        if (cell.owner != null) {
            backend.drawPolygon(
                shape = point.createHex(hexSize * theme.occupiedCellScale),
                color = cellColor,
            )
        }

        drawCellAccents(point, hex, cell)

        val label = cell.labelText(defaultShowTurnLabels = false) ?: return
        val labelColor = when {
            cell.owner == null -> theme.emptyCellLabelColor
            cellColor.isDark() -> cellColor.brighter()
            else -> cellColor.darker()
        }

        backend.drawString(
            point = point,
            text = label,
            maxWidth = hexSize * SQRT3 - 4 * borderThickness,
            fontSize = hexSize.toFloat() * 0.7f,
            font = FontType.SansSerifBold,
            color = labelColor,
        )
    }

    private fun drawCellAccents(point: Point, hex: Polygon, cell: Cell): Unit = context.run {
        val highlightColor = cell.highlight?.let { theme.run { it.color.color(highlightColor) } }

        if (highlightColor != null) {
            val highlightHex = point.createHex((hexSize - borderThickness * 2 / SQRT3).coerceAtLeast(0.0))

            backend.drawPolygon(
                shape = hex,
                color = highlightColor.withAlpha(64),
                borderRadius = borderThickness / 4,
            )

            if (cell.owner != null) {
                backend.drawCircle(point, Stroke(theme.focusColor, borderThickness * 8))
            }

            backend.drawPolygon(
                shape = highlightHex,
                color = Color.Transparent,
                outline = Stroke(highlightColor, borderThickness * 2.5f),
                borderRadius = borderThickness / 4,
            )
        }

        if (cell.focused || (maxTurn != null && cell.turn == maxTurn)) {
            val focusHex = point.createHex((hexSize * theme.occupiedCellScale - borderThickness * 2 / SQRT3).coerceAtLeast(0.0))

            backend.drawPolygon(
                shape = focusHex,
                color = theme.focusColor.withAlpha(64),
                borderRadius = borderThickness / 4,
            )

            backend.drawPolygon(
                shape = focusHex,
                color = Color.Transparent,
                outline = Stroke(theme.focusColor, borderThickness * 2),
                borderRadius = borderThickness / 4,
            )
        }
    }

    private fun CixTheme.emptyCellColor(point: Point): Color {
        val coordinate = context.layout.run { point.toCoordinate() }
        val distance = occupiedCoordinates.minOfOrNull { it.distanceTo(coordinate) }
            ?: return furthestEmptyCellColor

        val factor = if (emptyCellGradientRadius <= 1.0) {
            if (distance <= 1) 1.0 else 0.0
        } else {
            ((emptyCellGradientRadius - distance) / (emptyCellGradientRadius - 1.0))
                .coerceIn(0.0, 1.0)
        }

        fun interpolate(furthest: Int, nearest: Int) = (furthest + (nearest - furthest) * factor).roundToInt()

        return Color.of(
            red = interpolate(furthestEmptyCellColor.red, nearestEmptyCellColor.red),
            green = interpolate(furthestEmptyCellColor.green, nearestEmptyCellColor.green),
            blue = interpolate(furthestEmptyCellColor.blue, nearestEmptyCellColor.blue),
            alpha = interpolate(furthestEmptyCellColor.alpha, nearestEmptyCellColor.alpha),
        )
    }

    override fun drawLineHighlight(lineHighlight: LineHighlight) = context.run {
        val color = theme.run { lineHighlight.color.color(highlightColor) }
        backend.drawLine(
            from = lineHighlight.start.toPixel(),
            to = lineHighlight.endInclusive.toPixel(),
            stroke = Stroke(color, lineThickness),
            outline = Stroke(theme.boardBorderColor.withAlpha(128), lineThickness / 3),
        )
    }
}
