package de.mineking.hexo.board.render.image.theme

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.endInclusive
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.SQRT3
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.core.CellOwner

data class HDSTheme(
    override val gap: Double,
    val borderThickness: Double,
    override val backgroundColor: Color,
    val cellBorderColor: Color,
    val highlightColor: Color,
    val focusColor: Color,
    val emptyCellBackgroundColor: Color,
    val emptyCellLabelColor: Color,
    override val playerXColor: Color,
    override val playerOColor: Color,
) : BaseTheme() {
    companion object {
        val Default = HDSTheme(
            gap = 6.0,
            borderThickness = 2.0,
            backgroundColor = Color.rgb(0x0f172a),
            cellBorderColor = Color.rgb(0x232d43),
            highlightColor = Color.rgb(0xec6fb1),
            focusColor = Color.rgb(0xffffff),
            emptyCellBackgroundColor = Color.Transparent,
            emptyCellLabelColor = Color.rgb(0xb1c1e0),
            playerXColor = Color.rgb(0xfbc139),
            playerOColor = Color.rgb(0x38bdf8),
        )
    }

    override fun renderer(context: RenderingContext) = HDSRenderer(context, this)

    fun CellOwner?.color(default: Color, transform: (Color) -> Color = { it }) = when (this) {
        CellOwner.X -> transform(playerXColor)
        CellOwner.O -> transform(playerOColor)
        else -> default
    }
}

class HDSRenderer(
    context: RenderingContext,
    private val theme: HDSTheme,
) : BaseTheme.Renderer(context) {
    private val borderThickness = context.run { theme.borderThickness.relativeWidth() }
    private val labelPattern = """^(\s*#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8}))\s+(.+)$""".toRegex()

    override fun drawCell(point: Point, hex: Polygon, cell: Cell): Unit = context.run {
        backend.drawPolygon(
            shape = hex,
            color = theme.run { cell.owner.color(default = emptyCellBackgroundColor) },
            outline = Stroke(theme.cellBorderColor, borderThickness),
        )

        if (cell.highlight != null && cell.owner != null) {
            backend.drawLine(point, point, Stroke(theme.focusColor, borderThickness * 8))
        }

        drawCellHighlight(cell, hex)

        drawLabel(point, cell)
    }

    private fun drawLabel(point: Point, cell: Cell) {
        val label = cell.labelText(defaultShowTurnLabels = false) ?: return
        val match = labelPattern.matchEntire(label)

        val drawText: String
        val drawColor: Color

        if (match != null) {
            drawText = match.groupValues[2]
            drawColor = Color.parse(match.groupValues[1])
        } else {
            drawText = label
            drawColor = theme.run { cell.owner.color(default = emptyCellLabelColor) { it.darker() } }
        }

        context.backend.drawString(
            point = point,
            text = drawText,
            maxWidth = context.hexSize * SQRT3 - 4 * borderThickness,
            fontSize = context.hexSize.toFloat() * 0.7f,
            font = FontType.SansSerifBold,
            color = drawColor,
        )
    }

    private fun drawCellHighlight(cell: Cell, hex: Polygon): Unit = context.run {
        val color = when {
            cell.highlight != null -> theme.run { cell.highlight?.color.color(default = highlightColor) }
            cell.focused || (maxTurn != null && cell.turn == maxTurn) -> theme.focusColor
            else -> return
        }

        val borderThickness = borderThickness * 3
        backend.drawPolygon(hex, color.withAlpha(48), Stroke(color, borderThickness), borderRadius = borderThickness / 4f)
    }

    override fun drawLineHighlight(lineHighlight: LineHighlight) = context.run {
        val backgroundColor = theme.run { lineHighlight.color.color(default = highlightColor) }.withAlpha(240)
        val borderColor = theme.cellBorderColor.withAlpha(128)

        val thickness = borderThickness * 8
        context.backend.drawLine(
            from = lineHighlight.start.toPixel(),
            to = lineHighlight.endInclusive.toPixel(),
            stroke = Stroke(backgroundColor, thickness),
            outline = Stroke(borderColor, thickness / 3),
        )
    }
}
