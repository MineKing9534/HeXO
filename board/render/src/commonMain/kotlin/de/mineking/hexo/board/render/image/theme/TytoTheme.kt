package de.mineking.hexo.board.render.image.theme

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createHex

class TytoTheme(
    override val gap: Double,
    val borderThickness: Double,
    override val backgroundColor: Color,
    val emptyCellBackgroundColor: Color,
    val emptyCellBorderColor: Color,
    val occupiedCellBorderColor: Color,
    override val playerXColor: Color,
    override val playerOColor: Color,
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
        renderer.render(context, middleLayer)

        renderer.finalize()
    }

    fun Cell.backgroundColor() = when (owner) {
        CellOwner.X -> playerXColor
        CellOwner.O -> playerOColor
        null -> emptyCellBackgroundColor
    }
}

class TytoRenderer(
    context: RenderingContext,
    private val theme: TytoTheme,
) : BaseTheme.Renderer(context) {
    private val occupiedCells = mutableSetOf<Polygon>()
    private val focusedCells = mutableSetOf<Pair<Point, Color>>()

    private val borderThickness = context.run { theme.borderThickness.relativeWidth() }

    override fun drawCell(point: Point, hex: Polygon, cell: Cell): Unit = context.run {
        val color = theme.run { cell.backgroundColor() }
        backend.drawPolygon(
            shape = hex,
            color = color,
            outline = Stroke(theme.emptyCellBorderColor, borderThickness),
        )

        val turn = cell.turn
        if (turn != null && (turn == maxTurn || turn + 1 == maxTurn)) {
            val hex = point.createHex(hexSize * 0.75)
            backend.drawPolygon(hex, Color.Transparent, Stroke(color.brighter().withAlpha(196), borderThickness * 3))
        }

        if (cell.owner == null) return
        if (cell.focused) {
            focusedCells += point to color
        } else {
            occupiedCells += hex
        }
    }

    override fun drawLineHighlight(lineHighlight: LineHighlight) {
        // Not supported
    }

    fun finalize() = context.run {
        occupiedCells.forEach { hex ->
            backend.drawPolygon(
                shape = hex,
                color = Color.Transparent,
                outline = Stroke(theme.occupiedCellBorderColor, borderThickness * 2),
            )
        }

        focusedCells.forEach { (point, color) ->
            val color = color.adjustBrightness(3.5)
            backend.drawPolygon(
                shape = point.createHex(hexSize + borderThickness),
                color = color.withAlpha(48),
                outline = Stroke(color, borderThickness * 3),
            )
        }
    }
}
