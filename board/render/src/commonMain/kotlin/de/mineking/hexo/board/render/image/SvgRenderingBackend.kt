package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.Theme
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
        private val TEXT_MASK = SvgId("text-mask")
    }

    @Suppress("ContextReceiverMapping")
    private val textMask = mutableListOf<context(TagConsumer<*>, @SvgTagDSL Mask) () -> Unit>()
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
            createMask(TEXT_MASK) {
                textMask.forEach { it() }
            }
        }

        g {
            mask(FULL_MASK)
            mainContent.forEach { it() }
        }
    }

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) = configure {
        fun drawLinePart(stroke: Stroke) {
            // For some reason drawing a line with the same start and end point doesn't work properly
            if (from == to) {
                circle {
                    cx = from.x.none
                    cy = from.y.none

                    fill(stroke.color.svg)
                    r = (stroke.width / 2).none

                    mask(TEXT_MASK)
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

                    mask(TEXT_MASK)
                }
            }
        }

        if (outline != null) drawLinePart(Stroke(outline.color, stroke.width + outline.width))
        drawLinePart(stroke)
    }

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
        val font = InternalFontType.forFont(font)

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

                fontWeight = font.fontWeight
                fontFamily = font.fontFamily
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

private enum class InternalFontType(
    val fontFamily: String,
    val fontWeight: FontWeight,
) {
    SansSerifBold(
        fontFamily = "system-ui, -apple-system, BlinkMacSystemFont, sans-serif",
        fontWeight = FontWeight.Numeric(800),
    ) {
        private val firstPrintableCharacterCode = 32
        private val unknownCharacterWidth = 1.05
        private val openSansExtraBoldCharacterWidths = floatArrayOf(
            0.259766f, 0.290039f, 0.523926f, 0.662109f, 0.585938f, 0.948242f, 0.798828f, 0.291992f,
            0.358887f, 0.358887f, 0.537109f, 0.565918f, 0.309082f, 0.317871f, 0.289063f, 0.441895f,
            0.585938f, 0.585938f, 0.585938f, 0.585938f, 0.585938f, 0.585938f, 0.585938f, 0.585938f,
            0.585938f, 0.585938f, 0.289063f, 0.296875f, 0.565918f, 0.565918f, 0.565918f, 0.504883f,
            0.896973f, 0.726074f, 0.673828f, 0.648926f, 0.733887f, 0.548828f, 0.539063f, 0.740234f,
            0.766113f, 0.348145f, 0.352051f, 0.687012f, 0.582031f, 0.966797f, 0.833984f, 0.796875f,
            0.631836f, 0.796875f, 0.676758f, 0.577148f, 0.590820f, 0.756836f, 0.693848f, 1.039063f,
            0.723145f, 0.664063f, 0.610840f, 0.324219f, 0.441895f, 0.324219f, 0.524902f, 0.500000f,
            0.598145f, 0.623047f, 0.643066f, 0.539063f, 0.643066f, 0.618164f, 0.413086f, 0.605957f,
            0.669922f, 0.325195f, 0.324219f, 0.659180f, 0.323242f, 1.000000f, 0.669922f, 0.637207f,
            0.643066f, 0.643066f, 0.469238f, 0.533203f, 0.459961f, 0.669922f, 0.610840f, 0.910156f,
            0.629883f, 0.609863f, 0.506836f, 0.433105f, 0.508789f, 0.433105f, 0.565918f,
        )

        override fun estimateTextWidth(text: String) = text.sumOf { character ->
            openSansExtraBoldCharacterWidths
                .getOrNull(character.code - firstPrintableCharacterCode)
                ?.toDouble()
                ?: unknownCharacterWidth
        }
    },
    MonospaceRegular(
        fontFamily = "consolas, monospace, sans-serif",
        fontWeight = FontWeight.Normal,
    ) {
        private val characterWidth = 0.6
        override fun estimateTextWidth(text: String) = text.length * characterWidth
    },
    ;

    abstract fun estimateTextWidth(text: String): Double

    companion object {
        fun forFont(font: FontType) = when (font) {
            FontType.SansSerifBold -> SansSerifBold
            FontType.MonospaceRegular -> MonospaceRegular
        }
    }
}

private class SvgRgbaColor(color: Color) : SvgColor {
    override val svgString = "rgba(${color.red}, ${color.green}, ${color.blue}, ${color.alpha.toInt() / 255.0})"
}
