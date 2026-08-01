package de.mineking.hexo.board.render.image.tikz

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.image.BoardRenderBounds
import de.mineking.hexo.board.render.image.BoardRenderingHook
import de.mineking.hexo.board.render.image.BoundingBox
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.PolygonPath
import de.mineking.hexo.board.render.image.RenderingBackend
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.bottomRight
import de.mineking.hexo.board.render.image.createRenderLayout
import de.mineking.hexo.board.render.image.drawBoard
import de.mineking.hexo.board.render.image.minus
import de.mineking.hexo.board.render.image.pad
import de.mineking.hexo.board.render.image.plus
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.render.image.times
import de.mineking.hexo.board.render.image.toPath
import de.mineking.hexo.board.render.image.topLeft
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun Board.renderToTikZ(
    padding: Int,
    layoutRadius: Double = 64.0,
    visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
    theme: Theme = Theme.Default,
    renderingHook: BoardRenderingHook? = null,
    rawLabels: Boolean = true,
    labelStyle: String = "",
): String {
    require(cells.isNotEmpty())

    val layout = createRenderLayout(layoutRadius, BoardRenderBounds.Compact, visibleRadius)
    val bounds = layout.boundingBox.pad(padding)
    val backend = TikZRenderingBackend(rawLabels, labelStyle)
    backend.drawBoard(layout.copy(boundingBox = bounds), theme, renderingHook)

    val viewport = rectangleTikZPath(bounds.topLeft, bounds.bottomRight)

    val picture = tikzPicture(PICTURE_SCALE_OPTIONS) {
        path(viewport, theme.backgroundColor.fillOptions())
        scope {
            clip(viewport)
            backend.appendTo(this, viewport, bounds)
        }
    }

    // PGF caches an installed fading by name. Reusing one fixed name makes later boards use the
    // first board's text mask, so advance a TeX-side counter before declaring this board's mask.
    return if (backend.hasTextFading) TEXT_MASK_COUNTER_STEP + picture else picture
}

class TikZRenderingBackend(
    private val rawLabels: Boolean = true,
    private val labelStyle: String = "",
) : RenderingBackend {
    private val polygonCommands = mutableListOf<PolygonCommand>()
    private val lineCommands = mutableListOf<LineCommand>()
    private val textMask = mutableListOf<TextMaskEntry>()
    private val textCommands = mutableListOf<TikZCommand>()

    private data class LineCommand(val from: Point, val to: Point, val stroke: Stroke)
    private data class PolygonCommand(val path: TikZPath, val options: List<String>)
    private data class TextMaskEntry(val point: Point, val command: TikZCommand)

    val hasTextFading get() = lineCommands.isNotEmpty() && textMask.isNotEmpty()

    fun appendTo(picture: TikZPicture, viewport: TikZPath, bounds: BoundingBox) {
        picture.appendPolygons(polygonCommands)
        val fadingOptions = picture.declareTextFading(bounds)

        picture.scope {
            clip(viewport)
            lineCommands.forEach { line -> drawLinePart(line.from, line.to, line.stroke, fadingOptions) }
        }
        picture.append(textCommands)
    }

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        fun addLine(value: Stroke) {
            lineCommands += LineCommand(from, to, value)
        }

        if (outline != null) addLine(Stroke(outline.color, stroke.width + outline.width))
        addLine(stroke)
    }

    override fun drawPolygon(shape: Polygon, color: Color, outline: Stroke?, borderRadius: Float) {
        val polygon = shape.toPath(borderRadius).toTikZPath()
        polygonCommands += PolygonCommand(polygon, color.fillOptions() + outline.strokeOptions())
    }

    override fun drawString(
        point: Point,
        text: String,
        maxWidth: Double,
        fontSize: Float,
        font: FontType,
        color: Color,
    ) {
        val effectiveSize = fittedFontSize(text, maxWidth, fontSize, font)
        if (rawLabels) {
            drawRawLabel(point, text)
        } else {
            drawGlyphLabel(point, text, effectiveSize, font, color)
        }
    }

    private fun drawRawLabel(point: Point, text: String) {
        textCommands += { nodeRaw(point.tikz, text.withLabelStyle(), TEXT_SPACING_OPTIONS) }
        textMask += TextMaskEntry(point) {
            nodeRaw(
                point.tikz,
                "{$labelStyle ${textMaskHaloContent(text.withoutColor())}}",
                TEXT_SPACING_OPTIONS,
            )
            nodeRaw(point.tikz, rawLabelMaskContent(text), TEXT_SPACING_OPTIONS)
        }
    }

    private fun drawGlyphLabel(
        point: Point,
        text: String,
        effectiveSize: Float,
        font: FontType,
        color: Color,
    ) {
        val fontOptions = textOptions(effectiveSize, font)

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
        addGlyphLabelMask(point, text, effectiveSize, font)
    }

    private fun String.withLabelStyle() = if (labelStyle.isEmpty()) this else "{$labelStyle $this}"

    private fun addGlyphLabelMask(point: Point, text: String, effectiveSize: Float, font: FontType) {
        val fontOption = fontOption(effectiveSize, font)
        textMask += TextMaskEntry(point) {
            nodeRaw(point.tikz, textMaskHaloContent(escapeTikZ(text)), TEXT_SPACING_OPTIONS + fontOption)
            node(point.tikz, text, TEXT_SPACING_OPTIONS + listOf("text=transparent", fontOption))
        }
    }

    private fun textMaskHaloContent(text: String): String {
        val color = TEXT_MASK_HALO_COLOR
        val stroke = "${color.red / 255.0} ${color.green / 255.0} ${color.blue / 255.0}"
        val literal = { value: String ->
            "\\ifdefined\\pdfextension\\pdfextension literal{$value}\\else\\pdfliteral{$value}\\fi"
        }
        return "\\pgfsetlinewidth{\\dimexpr\\fontdimen6\\font/5\\relax}" +
            literal("q 2 Tr $stroke RG 1 J 1 j") + " " +
            text +
            literal("0 Tr Q")
    }

    private fun String.withoutColor() = "\\hexolabelwithoutcolor{$this}"

    private fun rawLabelMaskContent(text: String) =
        "{$labelStyle \\color{transparent} ${text.withoutColor()}}"

    private fun TikZPicture.declareTextFading(bounds: BoundingBox): List<String> {
        if (!hasTextFading) return emptyList()

        declareFading(TEXT_MASK_NAME, PICTURE_SCALE_OPTIONS) {
            val halfExtent = textMaskHalfExtent(bounds)
            path(
                rectangleTikZPath(Point(-halfExtent, -halfExtent), Point(halfExtent, halfExtent)),
                Color.rgb(0xffffff).fillOptions(),
            )
            textMask.forEach { it.command(this) }
        }
        return TEXT_FADING_OPTIONS
    }

    // PGF centers a fading picture on its origin. A symmetric background prevents the fading from
    // shifting while still covering the viewport and every text node that can cut into a line.
    private fun textMaskHalfExtent(bounds: BoundingBox): Double {
        var halfExtent = maxOf(abs(bounds.minX), abs(bounds.maxX), abs(bounds.minY), abs(bounds.maxY))
        textMask.forEach { entry ->
            halfExtent = max(halfExtent, max(abs(entry.point.x), abs(entry.point.y)))
        }
        return halfExtent + TEXT_MASK_MARGIN
    }

    private fun TikZPicture.appendPolygons(commands: List<PolygonCommand>) {
        var start = 0
        while (start < commands.size) {
            val options = commands[start].options
            var end = start + 1
            while (end < commands.size && commands[end].options == options) end++

            paths(commands.subList(start, end).map { it.path }, options)
            start = end
        }
    }
}

private typealias TikZCommand = TikZPicture.() -> Unit
private fun TikZPicture.append(commands: List<TikZCommand>) {
    commands.forEach { it() }
}

private fun FontType.tikzOption() = when (this) {
    FontType.SansSerifBold -> "\\sffamily\\bfseries"
    FontType.MonospaceRegular -> "\\ttfamily\\mdseries"
}

private fun fontOption(fontSize: Float, font: FontType): String {
    val sizeBp = fontSize * CSS_PIXEL_IN_BP
    return "font={${font.tikzOption()}\\fontsize{${sizeBp.tikzNumber()}bp}{${sizeBp.tikzNumber()}bp}\\selectfont}"
}

private fun textOptions(fontSize: Float, font: FontType) = TEXT_SPACING_OPTIONS + fontOption(fontSize, font)

private fun fittedFontSize(text: String, maxWidth: Double, fontSize: Float, font: FontType): Float {
    val estimatedWidth = font.estimateTextWidth(text) * fontSize
    return if (estimatedWidth > 0.0) {
        fontSize * min(1.0, maxWidth / estimatedWidth).toFloat()
    } else {
        fontSize
    }
}

private fun TikZPicture.drawLinePart(from: Point, to: Point, stroke: Stroke, fadingOptions: List<String> = emptyList()) {
    if (from == to) {
        circle(
            from.tikz,
            stroke.width * CSS_PIXEL_IN_BP / 2.0,
            listOf(
                "fill=${stroke.color.tikzColor()}",
                "fill opacity=${stroke.color.opacity()}",
                "draw=none",
            ) + fadingOptions,
        )
    } else {
        line(from.tikz, to.tikz, stroke.strokeOptions() + fadingOptions)
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

private fun PolygonPath.toTikZPath(): TikZPath {
    var current = start
    val tikzSegments = segments.map { segment ->
        when (segment) {
            is PolygonPath.Segment.Line -> TikZPathSegment.Line(segment.to.tikz).also { current = segment.to }
            is PolygonPath.Segment.QuadraticCurve -> {
                // TikZ's `controls ... and ...` is cubic. Convert the quadratic
                // control point so rounded polygons retain their intended shape.
                val control1 = current + (segment.control - current) * (2.0 / 3.0)
                val control2 = segment.to + (segment.control - segment.to) * (2.0 / 3.0)
                TikZPathSegment.CubicCurve(control1.tikz, control2.tikz, segment.to.tikz)
                    .also { current = segment.to }
            }
        }
    }
    return TikZPath(start.tikz, tikzSegments, closed = true)
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

private fun Color.tikzColor() = "{rgb,255:red,$red;green,$green;blue,$blue}"
private fun Color.opacity() = (alpha / 255.0).tikzNumber()

private val TEXT_SPACING_OPTIONS = listOf(
    "anchor=center",
    "inner sep=0bp",
    "outer sep=0pt",
)
private const val TEXT_MASK_NAME = "hexotextmask\\the\\hexotextmaskid"
private const val TEXT_MASK_COUNTER_STEP =
    "\\ifcsname hexotextmaskid\\endcsname" +
        "\\global\\advance\\hexotextmaskid by1\\relax" +
        "\\else\\newcount\\hexotextmaskid\\global\\hexotextmaskid=1\\relax\\fi\n"

private val TEXT_MASK_HALO_COLOR = Color.rgb(0x333333)
private const val TEXT_MASK_MARGIN = 64.0
private const val CSS_PIXEL_IN_BP = 0.75
private val PICTURE_SCALE_OPTIONS = listOf(
    "x=${CSS_PIXEL_IN_BP.tikzNumber()}bp",
    "y=-${CSS_PIXEL_IN_BP.tikzNumber()}bp",
)
private val TEXT_FADING_OPTIONS = listOf("path fading=$TEXT_MASK_NAME", "fit fading=false")
