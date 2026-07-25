package de.mineking.hexo.board.render.image.theme

import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.createHex
import kotlinx.serialization.Serializable

abstract class Theme {
    abstract val gap: Double
    abstract val backgroundColor: Color

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
        context.layout.coordinates.forEach {
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
}
