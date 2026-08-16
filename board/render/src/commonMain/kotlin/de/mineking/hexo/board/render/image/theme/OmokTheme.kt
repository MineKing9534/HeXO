package de.mineking.hexo.board.render.image.theme

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.endInclusive
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.SQRT3
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createHex
import de.mineking.hexo.board.render.image.div
import de.mineking.hexo.board.render.image.drawCircle
import de.mineking.hexo.board.render.image.plus

data class OmokTheme(
    override val gap: Double,
    val borderThickness: Double,
    val lineThickness: Double,
    override val backgroundColor: Color,
    val cellBorderColor: Color,
    val highlightColor: Color,
    val emptyCellBackgroundColor: Color,
    val emptyCellLabelColor: Color,
    override val playerXColor: Color,
    override val playerOColor: Color,
) : BaseTheme() {
    companion object {
        val Default = OmokTheme(
            gap = 6.0,
            borderThickness = 2.0,
            lineThickness = 16.0,
            backgroundColor = Color.rgb(0xcda577),
            cellBorderColor = Color.rgb(0x4c402c),
            highlightColor = Color.rgb(0xec6fb1),
            emptyCellBackgroundColor = Color.Transparent,
            emptyCellLabelColor = Color.rgb(0xb1c1e0),
            playerXColor = Color.rgb(0xf3f3f3),
            playerOColor = Color.rgb(0x141414),
        )
    }

    override fun renderer(context: RenderingContext) = OmokRenderer(context, this)

    fun CellOwner?.color(default: Color, transform: (Color) -> Color = { it }) = when (this) {
        CellOwner.X -> transform(playerXColor)
        CellOwner.O -> transform(playerOColor)
        else -> default
    }
}

class OmokRenderer(
    context: RenderingContext,
    private val theme: OmokTheme,
) : BaseTheme.Renderer(context) {
    private val borderThickness = context.run { theme.borderThickness.relativeWidth() }
    private val lineThickness = context.run { theme.lineThickness.relativeWidth() }

    override fun drawCell(point: Point, hex: Polygon, cell: Cell) = context.run {
        val gridHex = point.createHex(layout.size.layoutRadius)
        fun pointAt(index: Int) = (gridHex.points[index % 6] + gridHex.points[(index + 1) % 6]) / 2.0

        repeat(3) { index ->
            backend.drawLine(
                from = pointAt(index),
                to = pointAt(index + 3),
                stroke = Stroke(theme.cellBorderColor, borderThickness),
            )
        }

        backend.drawCircle(
            point = point,
            stroke = Stroke(theme.run { cell.owner.color(default = emptyCellBackgroundColor) }, (hexSize * SQRT3).toFloat()),
        )

        drawCellHighlight(point, cell)

        if (cell.highlight != null && cell.owner != null) {
            backend.drawCircle(point, Stroke(cell.focusColor, borderThickness * 8))
        }
    }

    private val Cell.focusColor get() = theme.run { owner?.other.color(default = Color.rgb(0xffffff)) }

    private fun drawCellHighlight(point: Point, cell: Cell) = context.run {
        val color = when {
            cell.highlight != null -> theme.run { cell.highlight?.color.color(default = highlightColor) }
            cell.focused || (maxTurn != null && cell.turn == maxTurn) -> cell.focusColor
            else -> return@run
        }

        backend.drawCircle(
            point = point,
            stroke = Stroke(color = color.withAlpha(if (cell.owner == null) 150 else 16), (hexSize * SQRT3).toFloat()),
            outline = Stroke(color, borderThickness * 4),
        )
    }

    override fun drawLineHighlight(lineHighlight: LineHighlight) = context.run {
        val backgroundColor = theme.run { lineHighlight.color.color(default = highlightColor) }.withAlpha(240)
        val borderColor = theme.cellBorderColor.withAlpha(128)

        context.backend.drawLine(
            from = lineHighlight.start.toPixel(),
            to = lineHighlight.endInclusive.toPixel(),
            stroke = Stroke(backgroundColor, lineThickness),
            outline = Stroke(borderColor, lineThickness / 3),
        )
    }
}
