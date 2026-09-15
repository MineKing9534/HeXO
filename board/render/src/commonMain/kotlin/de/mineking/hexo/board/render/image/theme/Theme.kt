package de.mineking.hexo.board.render.image.theme

import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.RenderingBackend
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.SQRT3
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createHex
import de.mineking.hexo.board.render.image.drawCircle
import kotlinx.serialization.Serializable

enum class CellShape {
    Hexagon {
        override fun RenderingBackend.drawCellShape(
            point: Point,
            radius: Double,
            color: Color,
            outline: Stroke?,
            borderRadius: Float,
        ) = drawPolygon(point.createHex(radius), color, outline, borderRadius)
    },
    Circle {
        override fun RenderingBackend.drawCellShape(
            point: Point,
            radius: Double,
            color: Color,
            outline: Stroke?,
            borderRadius: Float,
        ) {
            val diameter = (radius * SQRT3).toFloat()
            val outlineWidth = outline?.width ?: 0f
            drawCircle(
                point = point,
                stroke = Stroke(color, diameter - outlineWidth),
                outline = outline,
            )
        }
    },
    ;

    abstract fun RenderingBackend.drawCellShape(
        point: Point,
        radius: Double,
        color: Color,
        outline: Stroke? = null,
        borderRadius: Float = 0f,
    )
}

abstract class Theme {
    abstract val gap: Double
    abstract val backgroundColor: Color
    open val cellShape = CellShape.Hexagon

    abstract fun render(context: RenderingContext, middleLayer: () -> Unit)

    companion object {
        val Default: Theme get() = HDSTheme.Default
    }
}

abstract class BaseTheme : Theme() {
    abstract class Renderer(val context: RenderingContext) {
        abstract fun drawCell(point: Point, hex: Polygon, cell: Cell)
        abstract fun drawLineHighlight(lineHighlight: LineHighlight)

        fun Cell.labelText(
            defaultShowTurnLabels: Boolean,
            turnTransform: (Int) -> Int = { it },
        ) = label
            .takeIf { it.isNotBlank() }
            ?: turn
                ?.let { "${turnTransform(it)}" }
                .takeIf { context.layout.board.attributes[BoardAttribute.ShowTurnNumbers] ?: defaultShowTurnLabels }
    }

    abstract val playerXColor: Color
    abstract val playerOColor: Color

    protected abstract fun renderer(context: RenderingContext): Renderer

    protected fun Renderer.render(context: RenderingContext, middleLayer: () -> Unit) = context.run {
        context.visibleCoordinates.forEach {
            val point = it.toPixel()
            val hex = point.createHex(context.hexSize)

            if (!hex.isVisible()) return@forEach

            val cell = context.layout.board.cells[it] ?: Cell.EMPTY
            drawCell(point, hex, cell)
        }

        middleLayer()
        context.layout.board.lineHighlights.forEach {
            drawLineHighlight(it)
        }
    }

    override fun render(context: RenderingContext, middleLayer: () -> Unit) {
        val renderer = renderer(context)
        renderer.render(context, middleLayer)
    }
}

@Serializable
enum class DefaultTheme(val theme: BaseTheme) {
    HDS(HDSTheme.Default),
    HTTTX(HTTTXTheme.Default),
    Tyto(TytoTheme.Default),
    Omok(OmokTheme.Default),
}
