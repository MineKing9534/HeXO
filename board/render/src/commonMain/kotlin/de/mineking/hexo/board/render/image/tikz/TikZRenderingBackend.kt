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
    rawLabels: Boolean = true,
    labelStyle: String = "",
    width: String? = "\\linewidth",
): String {
    require(cells.isNotEmpty())

    val layout = createRenderLayout(layoutRadius, BoardRenderBounds.Compact, visibleRadius)
    val bounds = layout.boundingBox.pad(padding)
    val backend = TikZRenderingBackend(theme.backgroundColor, rawLabels, labelStyle)
    backend.drawBoard(layout.copy(boundingBox = bounds), theme, renderingHook)

    val viewport = rectangleTikZPath(bounds.topLeft, bounds.bottomRight)

    return tikzPicture(
        options = listOf("x=${CSS_PIXEL_IN_BP.tikzNumber()}bp", "y=-${CSS_PIXEL_IN_BP.tikzNumber()}bp"),
        width = width,
    ) {
        path(viewport, theme.backgroundColor.fillOptions())
        scope {
            clip(viewport)
            backend.appendTo(this)
        }
    }
}

class TikZRenderingBackend(
    private val backgroundColor: Color = Color.Transparent,
    private val rawLabels: Boolean = true,
    private val labelStyle: String = "",
) : RenderingBackend {
    private val polygonCommands = mutableListOf<TikZPicture.() -> Unit>()
    private val lineCommands = mutableListOf<TikZPicture.() -> Unit>()
    private val textMaskCommands = mutableListOf<TikZPicture.() -> Unit>()
    private val textCommands = mutableListOf<TikZPicture.() -> Unit>()
    private var textBackgroundColor = backgroundColor

    fun appendTo(picture: TikZPicture) {
        polygonCommands.forEach { picture.it() }
        lineCommands.forEach { picture.it() }
        textMaskCommands.forEach { picture.it() }
        textCommands.forEach { picture.it() }
    }

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        fun addLine(value: Stroke) {
            lineCommands += {
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
        textBackgroundColor = when (color.alpha.toInt()) {
            0 -> backgroundColor
            255 -> color
            else -> textBackgroundColor
        }

        val polygon = shape.toPath(borderRadius).toTikZPath()
        polygonCommands += {
            path(polygon, color.fillOptions() + outline.strokeOptions())
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
        if (rawLabels) {
            drawRawLabel(point, text)
        } else {
            drawGlyphLabel(point, text, maxWidth, fontSize, font, color)
        }
    }

    private fun drawRawLabel(point: Point, text: String) {
        val styledText = text.withLabelStyle()
        addMaskNode(point, styledText.rawMask(textBackgroundColor), TEXT_SPACING_OPTIONS)
        addRawTextNode(point, styledText, TEXT_SPACING_OPTIONS)
    }

    private fun drawGlyphLabel(
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
        val fontOptions = TEXT_SPACING_OPTIONS + listOf(
            "font={$fontOption\\fontsize{${effectiveSizeBp.tikzNumber()}bp}{${effectiveSizeBp.tikzNumber()}bp}\\selectfont}",
        )

        addMaskNode(
            point,
            text.glyphMask(textBackgroundColor, effectiveSizeBp / GLYPH_MASK_WIDTH_FACTOR),
            fontOptions + "text opacity=0.8",
        )
        textCommands += {
            node(
                point.tikz,
                text,
                fontOptions + listOf(
                    "text=${color.tikzColor()}",
                    "text opacity=${color.opacity()}",
                ),
            )
        }
    }

    private fun String.withLabelStyle() = if (labelStyle.isEmpty()) this else "{$labelStyle $this}"

    private fun addMaskNode(point: Point, content: String, options: List<String>) {
        textMaskCommands += { nodeRaw(point.tikz, content, options) }
    }

    private fun addRawTextNode(point: Point, content: String, options: List<String>) {
        textCommands += { nodeRaw(point.tikz, content, options) }
    }
}

private val Point.tikz get() = TikZPoint(x, y)

private fun rectangleTikZPath(topLeft: Point, bottomRight: Point) = TikZPath(
    topLeft.tikz,
    listOf(
        TikZPathSegment.Line(Point(bottomRight.x, topLeft.y).tikz),
        TikZPathSegment.Line(bottomRight.tikz),
        TikZPathSegment.Line(Point(topLeft.x, bottomRight.y).tikz),
    ),
    closed = true,
)

private fun PolygonPath.toTikZPath() = TikZPath(
    start.tikz,
    segments.map {
        when (it) {
            is PolygonPath.Segment.Line -> TikZPathSegment.Line(it.to.tikz)
            is PolygonPath.Segment.QuadraticCurve -> TikZPathSegment.QuadraticCurve(it.control.tikz, it.to.tikz)
        }
    },
    closed = true,
)

private fun String.rawMask(color: Color) = buildString {
    append("{\\definecolor{hexolabelbackground}{RGB}{")
    append(color.rgbComponents())
    append("}\\textpdfrender{")
    append("TextRenderingMode=Stroke,")
    append("LineWidth=\\hexolabelmaskwidth,")
    append("StrokeColor=hexolabelbackground")
    append("}{\\hexolabelwithoutcolor{")
    append(this@rawMask)
    append("}}}")
}

private fun String.glyphMask(color: Color, strokeWidth: Double) = buildString {
    append("\\pdfextension literal page {q ")
    append(color.pdfStrokeColor())
    append(' ')
    append(strokeWidth.tikzNumber())
    append(" w 1 Tr}")
    append(escapeTikZ(this@glyphMask))
    append("\\pdfextension literal page {Q}")
}

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

private fun Color.rgbComponents() = "$red,$green,$blue"
private fun Color.pdfStrokeColor() = "${red.pdfColorComponent()} ${green.pdfColorComponent()} ${blue.pdfColorComponent()} RG"
private fun Color.tikzColor() = "{rgb,255:red,$red;green,$green;blue,$blue}"
private fun Color.opacity() = (alpha / 255.0).tikzNumber()
private fun Int.pdfColorComponent() = (this / 255.0).tikzNumber()

private val TEXT_SPACING_OPTIONS = listOf("inner sep=0bp", "outer sep=0pt")
private const val GLYPH_MASK_WIDTH_FACTOR = 6.0
private const val CSS_PIXEL_IN_BP = 0.75
