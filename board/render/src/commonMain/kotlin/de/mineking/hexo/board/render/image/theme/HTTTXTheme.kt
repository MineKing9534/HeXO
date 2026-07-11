package de.mineking.hexo.board.render.image.theme

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.end
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.SQRT3
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.minus
import de.mineking.hexo.board.render.image.plus
import de.mineking.hexo.core.CellOwner

data class HTTTXTheme(
    override val gap: Double,
    val borderThickness: Double,
    val lineThickness: Double,
    override val backgroundColor: Color,
    val cellBorderColor: Color,
    val occupiedHighlightColor: Color,
    val emptyHighlightColor: Color,
    val lineHighlightColor: Color,
    val playerXBackgroundColor: Color,
    val playerXDecorationColor: Color,
    val playerOBackgroundColor: Color,
    val playerODecorationColor: Color,
) : BaseTheme() {
    companion object {
        val Default = HTTTXTheme(
            gap = 1.0,
            borderThickness = 3.0,
            lineThickness = 16.0,
            backgroundColor = Color.rgb(0x1f1729),
            cellBorderColor = Color.rgb(0x24213a),
            emptyHighlightColor = Color.rgb(0x491628),
            occupiedHighlightColor = Color.rgb(0x651527),
            lineHighlightColor = Color.rgba(0x88b81f34),
            playerXBackgroundColor = Color.rgb(0x5e2f1d),
            playerXDecorationColor = Color.rgb(0xd25a06),
            playerOBackgroundColor = Color.rgb(0x163a65),
            playerODecorationColor = Color.rgb(0x0383e1),
        )
    }

    override val playerXColor = playerXBackgroundColor
    override val playerOColor = playerOBackgroundColor

    override fun renderer(context: RenderingContext) = HTTTXRenderer(context, this)

    fun Cell.backgroundColor() = when {
        highlight != null && owner == null -> emptyHighlightColor
        highlight != null && owner != null -> occupiedHighlightColor
        owner == CellOwner.X -> playerXBackgroundColor
        owner == CellOwner.O -> playerOBackgroundColor
        else -> Color.Transparent
    }

    fun Cell.decorationColor() = when {
        owner == CellOwner.X -> playerXDecorationColor
        owner == CellOwner.O -> playerODecorationColor
        highlight != null -> emptyHighlightColor.brighter()
        else -> backgroundColor.brighter()
    }
}

class HTTTXRenderer(
    context: RenderingContext,
    private val theme: HTTTXTheme,
) : BaseTheme.Renderer(context) {
    private val borderThickness = context.run { theme.borderThickness.relativeWidth() }
    private val lineThickness = context.run { theme.lineThickness.relativeWidth() }

    override fun drawCell(point: Point, hex: Polygon, cell: Cell) = context.run {
        val color = theme.run { cell.backgroundColor() }
        backend.drawPolygon(
            shape = hex,
            color = color,
            outline = Stroke(theme.cellBorderColor, borderThickness),
        )

        val labelText = cell.labelText(
            defaultShowTurnLabels = true,
            turnTransform = { (it + 1) / 2 },
        )

        if (labelText != null) {
            val labelColor = theme.run { cell.decorationColor() }
            backend.drawString(
                point = point,
                text = labelText,
                maxWidth = hexSize * SQRT3 - 4 * borderThickness,
                fontSize = hexSize.toFloat() * 0.85f,
                font = FontType.MonospaceRegular,
                color = labelColor,
            )
        } else {
            drawDecoration(point, cell, color)
        }
    }

    override fun drawLineHighlight(lineHighlight: LineHighlight) = context.run {
        context.backend.drawLine(
            from = lineHighlight.start.toPixel(),
            to = lineHighlight.end.toPixel(),
            stroke = Stroke(theme.lineHighlightColor, lineThickness),
        )
    }

    private fun drawDecoration(point: Point, cell: Cell, cellColor: Color): Unit = context.run {
        val thickness = lineThickness * 0.6f
        when (cell.owner) {
            CellOwner.X -> {
                fun drawXPart(direction: Point) {
                    backend.drawLine(
                        from = point - direction,
                        to = point + direction,
                        stroke = Stroke(theme.playerXDecorationColor, thickness),
                    )
                }

                val delta = hexSize * 0.42
                drawXPart(Point(delta, delta))
                drawXPart(Point(delta, -delta))
            }
            CellOwner.O -> {
                fun drawCircle(radius: Double, color: Color) {
                    backend.drawLine(
                        from = point,
                        to = point,
                        stroke = Stroke(color, radius.toFloat()),
                    )
                }

                val radius = hexSize * 1.2
                drawCircle(radius, theme.playerODecorationColor)
                drawCircle(radius - thickness * 2, cellColor)
            }
            else -> return
        }
    }
}
