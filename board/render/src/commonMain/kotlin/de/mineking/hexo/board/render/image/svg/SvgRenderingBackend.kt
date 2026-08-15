package de.mineking.hexo.board.render.image.svg

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.render.image.BoardRenderBounds
import de.mineking.hexo.board.render.image.BoardRenderingHook
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.Polygon
import de.mineking.hexo.board.render.image.PolygonPath
import de.mineking.hexo.board.render.image.RenderingBackend
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createRenderLayout
import de.mineking.hexo.board.render.image.drawBoard
import de.mineking.hexo.board.render.image.height
import de.mineking.hexo.board.render.image.minus
import de.mineking.hexo.board.render.image.pad
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.render.image.toPath
import de.mineking.hexo.board.render.image.topLeft
import de.mineking.hexo.board.render.image.width
import dev.jamesyox.svg4k.SvgTagDSL
import dev.jamesyox.svg4k.TagConsumer
import dev.jamesyox.svg4k.attr.AttributeConsumer
import dev.jamesyox.svg4k.attr.attrs.DominantBaseline
import dev.jamesyox.svg4k.attr.attrs.FontSize
import dev.jamesyox.svg4k.attr.attrs.FontWeight
import dev.jamesyox.svg4k.attr.attrs.MaskType
import dev.jamesyox.svg4k.attr.attrs.MaskUnits
import dev.jamesyox.svg4k.attr.attrs.StrokeLinecap
import dev.jamesyox.svg4k.attr.attrs.StrokeLinejoin
import dev.jamesyox.svg4k.attr.attrs.TextAnchor
import dev.jamesyox.svg4k.attr.attrs.ViewBox
import dev.jamesyox.svg4k.attr.attrs.cx
import dev.jamesyox.svg4k.attr.attrs.cy
import dev.jamesyox.svg4k.attr.attrs.d
import dev.jamesyox.svg4k.attr.attrs.dominantBaseline
import dev.jamesyox.svg4k.attr.attrs.fill
import dev.jamesyox.svg4k.attr.attrs.fontFamily
import dev.jamesyox.svg4k.attr.attrs.fontWeight
import dev.jamesyox.svg4k.attr.attrs.id
import dev.jamesyox.svg4k.attr.attrs.mask
import dev.jamesyox.svg4k.attr.attrs.maskType
import dev.jamesyox.svg4k.attr.attrs.maskUnits
import dev.jamesyox.svg4k.attr.attrs.points
import dev.jamesyox.svg4k.attr.attrs.r
import dev.jamesyox.svg4k.attr.attrs.stroke
import dev.jamesyox.svg4k.attr.attrs.strokeLinecap
import dev.jamesyox.svg4k.attr.attrs.strokeLinejoin
import dev.jamesyox.svg4k.attr.attrs.strokeWidth
import dev.jamesyox.svg4k.attr.attrs.textAnchor
import dev.jamesyox.svg4k.attr.attrs.viewBox
import dev.jamesyox.svg4k.attr.attrs.x
import dev.jamesyox.svg4k.attr.attrs.x1
import dev.jamesyox.svg4k.attr.attrs.x2
import dev.jamesyox.svg4k.attr.attrs.y
import dev.jamesyox.svg4k.attr.attrs.y1
import dev.jamesyox.svg4k.attr.attrs.y2
import dev.jamesyox.svg4k.attr.types.obj.SvgColor
import dev.jamesyox.svg4k.attr.types.obj.SvgId
import dev.jamesyox.svg4k.attr.types.obj.none
import dev.jamesyox.svg4k.attr.types.obj.pct
import dev.jamesyox.svg4k.attr.types.obj.px
import dev.jamesyox.svg4k.consumers.svgString
import dev.jamesyox.svg4k.tags.Group
import dev.jamesyox.svg4k.tags.Mask
import dev.jamesyox.svg4k.tags.categories.container.ElementContainer
import dev.jamesyox.svg4k.tags.categories.container.unaryPlus
import dev.jamesyox.svg4k.tags.circle
import dev.jamesyox.svg4k.tags.defs
import dev.jamesyox.svg4k.tags.g
import dev.jamesyox.svg4k.tags.line
import dev.jamesyox.svg4k.tags.mask
import dev.jamesyox.svg4k.tags.path
import dev.jamesyox.svg4k.tags.polygon
import dev.jamesyox.svg4k.tags.rect
import dev.jamesyox.svg4k.tags.svg
import dev.jamesyox.svg4k.tags.text
import dev.jamesyox.svg4k.util.ClosePath
import dev.jamesyox.svg4k.util.L
import dev.jamesyox.svg4k.util.M
import dev.jamesyox.svg4k.util.Q
import kotlin.math.min
import dev.jamesyox.svg4k.attr.attrs.fontSize as fs
import dev.jamesyox.svg4k.attr.attrs.height as h
import dev.jamesyox.svg4k.attr.attrs.width as w
import dev.jamesyox.svg4k.attr.types.obj.Point as SvgPoint

fun Board.renderToSvg(
    padding: Int,
    layoutRadius: Double = 64.0,
    visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
    prettyPrint: Boolean = false,
    theme: Theme = Theme.Default,
    renderingHook: BoardRenderingHook? = null,
): String {
    require(!isEmpty(includeHighlights = true))

    val layout = createRenderLayout(layoutRadius, BoardRenderBounds.Compact, visibleRadius)
    val width = layout.boundingBox.width + 2 * padding
    val height = layout.boundingBox.height + 2 * padding

    val topLeftCorner = layout.boundingBox.topLeft - Point(padding, padding)

    val context = SvgRenderingBackend(topLeftCorner)
    context.drawBoard(layout.copy(boundingBox = layout.boundingBox.pad(padding)), theme, renderingHook)

    return svgString(prettyPrint) {
        svg {
            viewBox = ViewBox(topLeftCorner.x, topLeftCorner.y, width, height)
            rect {
                x = topLeftCorner.x.none
                y = topLeftCorner.y.none
                w = 100.pct
                h = 100.pct
                fill(theme.backgroundColor.svg)
            }

            context.appendHere()
        }
    }
}

class SvgRenderingBackend(private val topLeftCorner: Point) : RenderingBackend {
    companion object {
        private val FULL_MASK = SvgId("full-mask")
    }

    @Suppress("ContextReceiverMapping")
    private val textMask = mutableListOf<context(TagConsumer<*>, @SvgTagDSL Mask) () -> Unit>()
    private val usedTextMaskSizes = mutableSetOf<Int>()
    @Suppress("ContextReceiverMapping")
    private val mainContent = mutableListOf<context(TagConsumer<*>, @SvgTagDSL Group) () -> Unit>()

    private fun configure(block: context(TagConsumer<*>, @SvgTagDSL Group) () -> Unit) {
        mainContent += block
    }

    context(_: TagConsumer<*>, _: T)
    fun <T> appendHere() where T : ElementContainer.Defs, T : ElementContainer.G {
        defs {
            fun createMask(name: SvgId, config: context(AttributeConsumer, @SvgTagDSL Mask) () -> Unit = {}) {
                mask {
                    x = topLeftCorner.x.none
                    y = topLeftCorner.y.none
                    w = 100.pct
                    h = 100.pct

                    id = name
                    maskType = MaskType.Luminance
                    maskUnits = MaskUnits.UserSpaceOnUse

                    rect {
                        x = topLeftCorner.x.none
                        y = topLeftCorner.y.none
                        w = 100.pct
                        h = 100.pct

                        fill(Color.rgb(0xffffff).svg)
                    }

                    config()
                }
            }

            createMask(FULL_MASK)
            usedTextMaskSizes.forEach { size ->
                createMask(textMaskId(size)) {
                    textMask.take(size).forEach { it() }
                }
            }
        }

        g {
            mask(FULL_MASK)
            mainContent.forEach { it() }
        }
    }

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        val textMaskSize = textMask.size
        if (textMaskSize > 0) usedTextMaskSizes += textMaskSize

        configure {
            fun drawLinePart(stroke: Stroke) {
                // For some reason drawing a line with the same start and end point doesn't work properly
                if (from == to) {
                    circle {
                        cx = from.x.none
                        cy = from.y.none

                        fill(stroke.color.svg)
                        r = (stroke.width / 2).none

                        if (textMaskSize > 0) mask(textMaskId(textMaskSize))
                    }
                } else {
                    line {
                        x1 = from.x.none
                        y1 = from.y.none
                        x2 = to.x.none
                        y2 = to.y.none

                        stroke(stroke.color.svg)
                        strokeWidth = stroke.width.none
                        strokeLinecap = StrokeLinecap.Round

                        if (textMaskSize > 0) mask(textMaskId(textMaskSize))
                    }
                }
            }

            if (outline != null) drawLinePart(Stroke(outline.color, stroke.width + outline.width))
            drawLinePart(stroke)
        }
    }

    private fun textMaskId(size: Int) = SvgId("text-mask-$size")

    override fun drawPolygon(shape: Polygon, color: Color, outline: Stroke?, borderRadius: Float) = configure {
        if (borderRadius <= 0f) {
            polygon {
                points = shape.points.map { (x, y) -> SvgPoint(x, y) }

                fill(color.svg)
                if (outline != null) {
                    stroke(outline.color.svg)
                    strokeWidth = outline.width.none
                }
            }
        } else {
            path {
                val path = shape.toPath(borderRadius)
                d {
                    M(path.start.x, path.start.y)
                    path.segments.forEach { segment ->
                        when (segment) {
                            is PolygonPath.Segment.Line -> L(segment.to.x, segment.to.y)
                            is PolygonPath.Segment.QuadraticCurve -> Q(
                                segment.control.x,
                                segment.control.y,
                                segment.to.x,
                                segment.to.y,
                            )
                        }
                    }
                    ClosePath
                }

                fill(color.svg)
                if (outline != null) {
                    stroke(outline.color.svg)
                    strokeWidth = outline.width.none
                }
            }
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
        val effectiveFontSize = fontSize * min(1.0, maxWidth / estimatedWidth).toFloat()

        context(_: TagConsumer<*>, _: ElementContainer.Text)
        fun drawText(color: Color?, stroke: Stroke?) {
            text {
                if (color != null) fill(color.svg)
                if (stroke != null) {
                    stroke(stroke.color.svg)
                    strokeWidth = stroke.width.none
                    strokeLinecap = StrokeLinecap.Round
                    strokeLinejoin = StrokeLinejoin.Round
                }

                when (font) {
                    FontType.SansSerifBold -> {
                        fontWeight = FontWeight.Numeric(800)
                        fontFamily = "system-ui, -apple-system, BlinkMacSystemFont, sans-serif"
                    }
                    FontType.MonospaceRegular -> {
                        fontWeight = FontWeight.Normal
                        fontFamily = "consolas, monospace, sans-serif"
                    }
                }
                fs = FontSize.Value(effectiveFontSize.px)

                textAnchor = TextAnchor.Middle
                dominantBaseline = DominantBaseline.Central

                x = listOf(point.x.none)
                y = listOf(point.y.none)

                +text
            }
        }

        configure { drawText(color, null) }
        textMask += {
            drawText(null, Stroke(Color.rgb(0x333333), effectiveFontSize / 6))
            drawText(Color.rgb(0x000000), null)
        }
    }
}

private val Color.svg get() = SvgRgbaColor(this)

private class SvgRgbaColor(color: Color) : SvgColor {
    override val svgString = "rgba(${color.red}, ${color.green}, ${color.blue}, ${color.alpha / 255.0})"
}
