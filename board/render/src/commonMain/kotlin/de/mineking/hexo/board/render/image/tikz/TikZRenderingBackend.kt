package de.mineking.hexo.board.render.image.tikz

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.image.BoardRenderBounds
import de.mineking.hexo.board.render.image.BoardRenderingHook
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.PolygonPath
import de.mineking.hexo.board.render.image.RenderingBackend
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.bottomRight
import de.mineking.hexo.board.render.image.createRenderLayout
import de.mineking.hexo.board.render.image.drawBoard
import de.mineking.hexo.board.render.image.pad
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.render.image.toPath
import de.mineking.hexo.board.render.image.topLeft
import kotlin.math.min

fun Board.renderToTikZ(
    padding: Int,
    layoutRadius: Double = 64.0,
    visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
    theme: Theme = Theme.Default,
    renderingHook: BoardRenderingHook? = null,
): String {
    require(cells.isNotEmpty())

    val layout = createRenderLayout(layoutRadius, BoardRenderBounds.Compact, visibleRadius)
    val bounds = layout.boundingBox.pad(padding)
    val backend = TikZRenderingBackend()
    backend.drawBoard(layout.copy(boundingBox = bounds), theme, renderingHook)

    val viewport = TikZPath(
        bounds.topLeft.tikz,
        listOf(
            TikZPathSegment.Line(Point(bounds.maxX, bounds.minY).tikz),
            TikZPathSegment.Line(bounds.bottomRight.tikz),
            TikZPathSegment.Line(Point(bounds.minX, bounds.maxY).tikz),
        ),
        closed = true,
    )

    return tikzPicture(listOf("x=${CSS_PIXEL_IN_BP.tikzNumber()}bp", "y=-${CSS_PIXEL_IN_BP.tikzNumber()}bp")) {
        path(
            viewport,
            theme.backgroundColor.fillOptions(),
        )
        scope {
            clip(viewport)
            backend.appendTo(this)
        }
    }
}

class TikZRenderingBackend : RenderingBackend {
    private val commands = mutableListOf<TikZPicture.() -> Unit>()
    private val textCommands = mutableListOf<TikZPicture.() -> Unit>()

    fun appendTo(picture: TikZPicture) {
        commands.forEach { picture.it() }
        textCommands.forEach { picture.it() }
    }

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        fun addLine(value: Stroke) {
            commands += {
                if (from == to) {
                    circle(
                        from.tikz,
                        value.width * CSS_PIXEL_IN_BP / 2.0,
                        listOf(
                            "fill=${value.color.tikzColor()}",
                            "fill opacity=${value.color.opacity()}",
                            "draw=none",
                        ),
                    )
                } else {
                    line(from.tikz, to.tikz, value.strokeOptions())
                }
            }
        }

        if (outline != null) addLine(Stroke(outline.color, stroke.width + outline.width))
        addLine(stroke)
    }

    override fun drawPolygon(shape: Polygon, color: Color, outline: Stroke?, borderRadius: Float) {
        val polygon = shape.toPath(borderRadius)
        commands += {
            path(
                TikZPath(
                    polygon.start.tikz,
                    polygon.segments.map {
                        when (it) {
                            is PolygonPath.Segment.Line -> TikZPathSegment.Line(it.to.tikz)
                            is PolygonPath.Segment.QuadraticCurve ->
                                TikZPathSegment.QuadraticCurve(it.control.tikz, it.to.tikz)
                        }
                    },
                    closed = true,
                ),
                color.fillOptions() + outline.strokeOptions(),
            )
        }
    }

    override fun drawString(
        point: Point,
        text: String,
        maxWidth: Double,
        fontSize: Float,
        font: FontType,
        color: Color,
    ) {
        val estimatedWidth = font.estimateTextWidth(text) * fontSize
        val effectiveSize = if (estimatedWidth > 0.0) {
            fontSize * min(1.0, maxWidth / estimatedWidth).toFloat()
        } else {
            fontSize
        }
        val fontOption = when (font) {
            FontType.SansSerifBold -> "\\sffamily\\bfseries"
            FontType.MonospaceRegular -> "\\ttfamily\\mdseries"
        }
        val effectiveSizeBp = effectiveSize * CSS_PIXEL_IN_BP

        textCommands += {
            node(
                point.tikz,
                text,
                listOf(
                    "text=${color.tikzColor()}",
                    "font={$fontOption\\fontsize{${effectiveSizeBp.tikzNumber()}bp}{${effectiveSizeBp.tikzNumber()}bp}\\selectfont}",
                    "inner sep=0bp",
                    "outer sep=0pt",
                    "text opacity=${color.opacity()}",
                ),
            )
        }
    }
}

private val Point.tikz get() = TikZPoint(x, y)

private fun Stroke?.strokeOptions(): List<String> = this?.let {
    listOf(
        "draw=${color.tikzColor()}",
        "draw opacity=${color.opacity()}",
        "line width=${(width * CSS_PIXEL_IN_BP).tikzNumber()}bp",
        "line cap=round",
        "line join=round",
    )
} ?: listOf("draw=none")

private fun Color.fillOptions() = listOf(
    "fill=${tikzColor()}",
    "fill opacity=${opacity()}",
)

private fun Color.tikzColor() = "{rgb,255:red,$red;green,$green;blue,$blue}"
private fun Color.opacity() = (alpha / 255.0).tikzNumber()

private const val CSS_PIXEL_IN_BP = 0.75
