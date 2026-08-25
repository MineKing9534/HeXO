package de.mineking.hexo.board.render.tikz

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
import kotlin.math.abs
import kotlin.math.min

fun Board.renderToTikZ(
    padding: Int,
    layoutRadius: Double = 64.0,
    visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
    theme: Theme = Theme.Default,
    renderingHook: BoardRenderingHook? = null,
    rawLabels: Boolean = true,
    compact: Boolean = true,
    labelStyle: String = "",
): String {
    require(cells.isNotEmpty())

    val layout = createRenderLayout(
        layoutRadius = layoutRadius,
        bounds = if (compact) BoardRenderBounds.Compact else BoardRenderBounds.IncludeSurroundings,
        visibleRadius = visibleRadius,
    )
    val bounds = layout.boundingBox.pad(padding)
    val backend = TikZRenderingBackend(rawLabels, labelStyle)
    backend.drawBoard(layout.copy(boundingBox = bounds), theme, renderingHook)
    return backend.render(bounds, theme.backgroundColor)
}

class TikZRenderingBackend(
    private val rawLabels: Boolean = true,
    private val labelStyle: String = "",
) : RenderingBackend {
    private val polygons = mutableListOf<StyledPath>()
    private val lines = mutableListOf<RenderedLine>()
    private val textMask = mutableListOf<String>()
    private val labels = mutableListOf<String>()
    private var textMaskExtent = 0.0

    private val hasTextFading get() = lines.isNotEmpty() && textMask.isNotEmpty()

    internal fun render(bounds: BoundingBox, background: Color): String {
        val viewport = rectanglePath(bounds)
        val fadingOptions = if (hasTextFading) TEXT_FADING_OPTIONS else emptyList()
        val commands = buildList {
            add(path(viewport, background.fillOptions()))
            scope {
                add(clip(viewport))
                addAll(polygons.commands())
                scope {
                    add(clip(viewport))
                    lines.forEach { add(lineCommand(it, fadingOptions)) }
                }
                addAll(labels)
            }
        }

        return buildString {
            if (hasTextFading) {
                // PGF caches installed fadings by name, so each board needs a fresh name.
                append(TEXT_MASK_COUNTER_STEP)
                append(textFading(bounds))
                append('\n')
            }
            append(tikzPicture(commands, PICTURE_SCALE_OPTIONS))
        }
    }

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        if (outline != null) lines += RenderedLine(from, to, Stroke(outline.color, stroke.width + outline.width))
        lines += RenderedLine(from, to, stroke)
    }

    override fun drawPolygon(shape: Polygon, color: Color, outline: Stroke?, borderRadius: Float) {
        polygons += StyledPath(shape.toPath(borderRadius).tikz, color.fillOptions() + outline.outlineOptions())
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
            return
        }

        val fontOption = fontOption(fittedFontSize(text, maxWidth, fontSize, font), font)
        labels += node(
            point,
            escapeTikZ(text),
            TEXT_SPACING_OPTIONS + fontOption + listOf(
                "text=${color.tikzColor()}",
                "text opacity=${color.opacity()}",
            ),
        )
        addToTextMask(
            point,
            node(point, textMaskHaloContent(escapeTikZ(text)), TEXT_SPACING_OPTIONS + fontOption),
            node(point, escapeTikZ(text), TEXT_SPACING_OPTIONS + listOf("text=transparent", fontOption)),
        )
    }

    private fun drawRawLabel(point: Point, text: String) {
        val styledText = if (labelStyle.isEmpty()) text else "{$labelStyle $text}"
        labels += node(point, styledText, TEXT_SPACING_OPTIONS)
        addToTextMask(
            point,
            node(point, "{$labelStyle ${textMaskHaloContent(text.withoutColor())}}", TEXT_SPACING_OPTIONS),
            node(point, "{$labelStyle \\color{transparent} ${text.withoutColor()}}", TEXT_SPACING_OPTIONS),
        )
    }

    private fun addToTextMask(point: Point, vararg commands: String) {
        textMask += commands
        textMaskExtent = maxOf(textMaskExtent, abs(point.x), abs(point.y))
    }

    private fun textFading(bounds: BoundingBox): String {
        // PGF centers a fading on its origin. A symmetric background keeps masks centered while
        // covering the viewport and labels that extend beyond it.
        val halfExtent = maxOf(
            abs(bounds.minX),
            abs(bounds.maxX),
            abs(bounds.minY),
            abs(bounds.maxY),
            textMaskExtent,
        ) + TEXT_MASK_MARGIN
        val background = rectanglePath(-halfExtent, -halfExtent, halfExtent, halfExtent)
        val commands = listOf(path(background, Color.rgb(0xffffff).fillOptions())) + textMask
        return "\\pgfdeclarefading{$TEXT_MASK_NAME}{%\n${tikzPicture(commands, PICTURE_SCALE_OPTIONS)}%\n}"
    }
}

private data class RenderedLine(val from: Point, val to: Point, val stroke: Stroke)
private data class StyledPath(val value: String, val options: List<String>)

private fun List<StyledPath>.commands() = buildList {
    var start = 0
    while (start < this@commands.size) {
        val options = this@commands[start].options
        val end = (start + 1 until this@commands.size)
            .firstOrNull { this@commands[it].options != options }
            ?: this@commands.size
        add(path(this@commands.subList(start, end).joinToString(" ") { it.value }, options))
        start = end
    }
}

private fun lineCommand(line: RenderedLine, fadingOptions: List<String>): String = with(line) {
    if (from == to) {
        path(
            "${from.tikz} circle[radius=${(stroke.width * CSS_PIXEL_IN_BP / 2.0).tikzNumber()}bp]",
            listOf(
                "fill=${stroke.color.tikzColor()}",
                "fill opacity=${stroke.color.opacity()}",
                "draw=none",
            ) + fadingOptions,
        )
    } else {
        path("${from.tikz} -- ${to.tikz}", stroke.strokeOptions() + fadingOptions)
    }
}

private fun MutableList<String>.scope(block: MutableList<String>.() -> Unit) {
    add("\\begin{scope}")
    block()
    add("\\end{scope}")
}

private fun tikzPicture(commands: List<String>, options: List<String>) = buildString {
    append("\\begin{tikzpicture}")
    append(options.tikz)
    append('\n')
    commands.forEach { append("  ").append(it).append('\n') }
    append("\\end{tikzpicture}")
}

private fun path(value: String, options: List<String> = emptyList()) = "\\path${options.tikz} $value;"
private fun clip(value: String) = "\\clip $value;"
private fun node(point: Point, content: String, options: List<String>) =
    "\\node${options.tikz} at ${point.tikz} {$content};"

private val List<String>.tikz get() = if (isEmpty()) "" else joinToString(prefix = "[", postfix = "]")
private val Point.tikz get() = "(${x.tikzNumber()}, ${y.tikzNumber()})"

private fun rectanglePath(bounds: BoundingBox) = rectanglePath(bounds.minX, bounds.minY, bounds.maxX, bounds.maxY)
private fun rectanglePath(minX: Double, minY: Double, maxX: Double, maxY: Double) = listOf(
    Point(minX, minY),
    Point(maxX, minY),
    Point(maxX, maxY),
    Point(minX, maxY),
).joinToString(" -- ", postfix = " -- cycle") { it.tikz }

private val PolygonPath.tikz: String get() = buildString {
    append(start.tikz)
    var current = start
    segments.forEach { segment ->
        when (segment) {
            is PolygonPath.Segment.Line -> {
                append(" -- ").append(segment.to.tikz)
                current = segment.to
            }
            is PolygonPath.Segment.QuadraticCurve -> {
                // TikZ curves are cubic, so convert the quadratic control point.
                val control1 = current + (segment.control - current) * (2.0 / 3.0)
                val control2 = segment.to + (segment.control - segment.to) * (2.0 / 3.0)
                append(" .. controls ").append(control1.tikz)
                    .append(" and ").append(control2.tikz)
                    .append(" .. ").append(segment.to.tikz)
                current = segment.to
            }
        }
    }
    append(" -- cycle")
}

private fun FontType.tikzOption() = when (this) {
    FontType.SansSerifBold -> "\\sffamily\\bfseries"
    FontType.MonospaceRegular -> "\\ttfamily\\mdseries"
}

private fun fontOption(fontSize: Float, font: FontType): String {
    val size = (fontSize * CSS_PIXEL_IN_BP).tikzNumber()
    return "font={${font.tikzOption()}\\fontsize{${size}bp}{${size}bp}\\selectfont}"
}

private fun fittedFontSize(text: String, maxWidth: Double, fontSize: Float, font: FontType): Float {
    val estimatedWidth = font.estimateTextWidth(text) * fontSize
    return if (estimatedWidth > 0.0) {
        fontSize * min(1.0, maxWidth / estimatedWidth).toFloat()
    } else {
        fontSize
    }
}

private fun Stroke?.outlineOptions(): List<String> = this?.strokeOptions() ?: listOf("draw=none")
private fun Stroke.strokeOptions() = listOf(
    "draw=${color.tikzColor()}",
    "draw opacity=${color.opacity()}",
    "line width=${(width * CSS_PIXEL_IN_BP).tikzNumber()}bp",
    "line cap=round",
    "line join=round",
)

private fun Color.fillOptions() = listOf(
    "fill=${tikzColor()}",
    "fill opacity=${opacity()}",
)

private fun Color.tikzColor() = "{rgb,255:red,$red;green,$green;blue,$blue}"
private fun Color.opacity() = (alpha / 255.0).tikzNumber()
private fun String.withoutColor() = "\\hexolabelwithoutcolor{$this}"

private fun textMaskHaloContent(text: String): String {
    val color = TEXT_MASK_HALO_COLOR
    val stroke = "${color.red / 255.0} ${color.green / 255.0} ${color.blue / 255.0}"
    fun literal(value: String) =
        "\\ifdefined\\pdfextension\\pdfextension literal{$value}\\else\\pdfliteral{$value}\\fi"

    return "\\pgfsetlinewidth{\\dimexpr\\fontdimen6\\font/5\\relax}" +
        literal("q 2 Tr $stroke RG 1 J 1 j") + " " + text + literal("0 Tr Q")
}

private fun Double.tikzNumber(): String {
    require(isFinite()) { "TikZ coordinates must be finite" }
    return toString().let { if (it == "-0.0") "0" else it.removeSuffix(".0") }
}

private fun escapeTikZ(value: String) = buildString {
    value.forEach { character ->
        append(
            when (character) {
                '\\' -> "\\textbackslash{}"
                '{' -> "\\{"
                '}' -> "\\}"
                '$' -> "\\$"
                '&' -> "\\&"
                '#' -> "\\#"
                '_' -> "\\_"
                '%' -> "\\%"
                '^' -> "\\textasciicircum{}"
                '~' -> "\\textasciitilde{}"
                else -> character
            },
        )
    }
}

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
private val TEXT_MASK_HALO_COLOR = Color.rgb(0x111111)
private const val TEXT_MASK_MARGIN = 48.0
private const val CSS_PIXEL_IN_BP = 0.75
private val PICTURE_SCALE_OPTIONS = listOf(
    "x=${CSS_PIXEL_IN_BP.tikzNumber()}bp",
    "y=-${CSS_PIXEL_IN_BP.tikzNumber()}bp",
)
private val TEXT_FADING_OPTIONS = listOf("path fading=$TEXT_MASK_NAME", "fit fading=false")
