package de.mineking.hexo.board.render.image.theme

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.endInclusive
import de.mineking.hexo.board.render.image.LineStyle
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.SQRT3
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createHex
import de.mineking.hexo.board.render.image.drawCircle

class TytoTheme(
    override val gap: Double,
    val borderThickness: Double,
    override val backgroundColor: Color,
    val emptyCellBackgroundColor: Color,
    val emptyCellBorderColor: Color,
    val occupiedCellBorderColor: Color,
    override val playerXColor: Color,
    override val playerOColor: Color,
    val lineHighlightColor: Color = Color.rgb(0xa78bfa),
) : BaseTheme() {
    companion object {
        val Default = TytoTheme(
            gap = 1.0,
            borderThickness = 2.0,
            backgroundColor = Color.rgb(0x0d0f0e),
            emptyCellBackgroundColor = Color.rgb(0x101211),
            emptyCellBorderColor = Color.rgb(0x2e2a1d),
            occupiedCellBorderColor = Color.rgb(0x3a423f),
            playerXColor = Color.rgb(0xf08a3c),
            playerOColor = Color.rgb(0x3fb6d9),
        )
    }

    override fun renderer(context: RenderingContext) = TytoRenderer(context, this)

    override fun render(context: RenderingContext, middleLayer: () -> Unit) {
        val renderer = renderer(context)
        renderer.render(context) {
            middleLayer()
            renderer.renderOverlays()
        }
    }

    fun Cell.backgroundColor() = when (owner) {
        CellOwner.X -> playerXColor
        CellOwner.O -> playerOColor
        null -> emptyCellBackgroundColor
    }

    fun CellOwner?.highlightColor() = when (this) {
        CellOwner.X -> playerXColor.darker(0.25)
        CellOwner.O -> playerOColor.darker(0.25)
        null -> lineHighlightColor
    }
}

class TytoRenderer(
    context: RenderingContext,
    private val theme: TytoTheme,
) : BaseTheme.Renderer(context) {
    private val occupiedCells = mutableSetOf<Polygon>()
    private val focusedCells = mutableSetOf<Pair<Point, Color>>()
    private val highlightedCells = mutableListOf<Pair<Point, Cell>>()

    private val borderThickness = context.run { theme.borderThickness.relativeWidth() }

    override fun drawCell(point: Point, hex: Polygon, cell: Cell): Unit = context.run {
        val color = theme.run { cell.backgroundColor() }
        backend.drawPolygon(
            shape = hex,
            color = color,
            outline = Stroke(theme.emptyCellBorderColor, borderThickness),
        )

        if (cell.highlight != null) {
            highlightedCells += point to cell
        }

        val turn = cell.turn
        if (turn != null && (turn == maxTurn || turn + 1 == maxTurn)) {
            val hex = point.createHex(hexSize * 0.75)
            backend.drawPolygon(hex, Color.Transparent, Stroke(color.brighter().withAlpha(196), borderThickness * 3))
        }

        drawLabel(
            point = point,
            cell = cell,
            color = if (color.isDark()) color.brighter() else color.darker(),
            maxWidth = hexSize * SQRT3 - 4 * borderThickness,
        )

        if (cell.owner == null) return
        if (cell.focused) {
            focusedCells += point to color
        } else {
            occupiedCells += hex
        }
    }

    private fun drawCellHighlight(point: Point, cell: Cell): Unit = context.run {
        if (borderThickness <= 0 || hexSize <= 0) return@run

        val highlight = cell.highlight ?: return@run
        val color = theme.run { highlight.color.highlightColor() }
        // Match both translucent background passes of the line endpoints.
        drawHighlightMarker(point, color, drawDot = false)
        drawHighlightMarker(point, color)
    }

    private fun drawHighlightMarker(point: Point, color: Color, drawDot: Boolean = true): Unit = context.run {
        backend.drawPolygon(
            shape = point.createHex(hexSize * 0.55),
            color = theme.emptyCellBackgroundColor.withAlpha(196),
            outline = Stroke(color, borderThickness * 4),
        )
        if (drawDot) {
            backend.drawCircle(point, Stroke(color, borderThickness * 4))
        }
    }

    override fun drawLineHighlight(lineHighlight: LineHighlight): Unit = context.run {
        if (borderThickness <= 0 || hexSize <= 0) return@run

        val color = theme.run { lineHighlight.color.highlightColor() }
        val start = lineHighlight.start.toPixel()
        val end = lineHighlight.endInclusive.toPixel()

        setOf(start, end).forEach { point ->
            drawHighlightMarker(point, color, drawDot = false)
        }

        backend.drawLine(
            from = start,
            to = end,
            stroke = Stroke(color.withAlpha(196), borderThickness * 2),
            style = LineStyle.Dashed,
        )

        setOf(start, end).forEach { point ->
            drawHighlightMarker(point, color)
        }
    }

    fun renderOverlays() = context.run {
        occupiedCells.forEach { hex ->
            backend.drawPolygon(
                shape = hex,
                color = Color.Transparent,
                outline = Stroke(theme.occupiedCellBorderColor, borderThickness * 2),
            )
        }

        focusedCells.forEach { (point, color) ->
            val color = color.brighter(0.75)
            backend.drawPolygon(
                shape = point.createHex(hexSize + borderThickness / 2),
                color = color.withAlpha(48),
                outline = Stroke(color, borderThickness * 3),
                borderRadius = borderThickness / 4,
            )
        }

        highlightedCells.forEach { (point, cell) ->
            drawCellHighlight(point, cell)
        }
    }
}
