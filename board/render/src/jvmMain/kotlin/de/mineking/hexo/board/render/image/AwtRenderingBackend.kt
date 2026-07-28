package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.render.image.theme.isTransparent
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import kotlin.math.min
import java.awt.Color as AwtColor

fun Board.renderToImage(
    layoutRadius: Double,
    padding: Int,
    visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
    theme: Theme = Theme.Default,
    middleLayer: RenderingContext.() -> Unit = {},
): BufferedImage {
    require(cells.isNotEmpty() || lineHighlights.isNotEmpty())

    val layout = createRenderLayout(layoutRadius, BoardRenderBounds.Compact, visibleRadius)
    val width = layout.boundingBox.width + 2 * padding
    val height = layout.boundingBox.height + 2 * padding

    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()

    graphics.color = theme.backgroundColor.awt
    graphics.fillRect(0, 0, width, height)

    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics.translate(padding - layout.boundingBox.minX, padding - layout.boundingBox.minY)

    val context = AwtRenderingBackend(graphics)
    try {
        context.drawBoard(layout.copy(boundingBox = layout.boundingBox.pad(padding)), theme, middleLayer)
    } finally {
        graphics.dispose()
    }

    return image
}

class AwtRenderingBackend(private val graphics: Graphics2D) : RenderingBackend {
    private data class TextExclusion(
        val shape: Shape,
        val margin: Shape,
        val bounds: Rectangle2D,
    )

    companion object {
        private val OPENSANS_BOLD_FONT = Font.createFont(Font.TRUETYPE_FONT, javaClass.getResourceAsStream("/fonts/open-sans.extrabold.ttf"))
        private val CONSOLAS_FONT = Font.createFont(Font.TRUETYPE_FONT, javaClass.getResourceAsStream("/fonts/consolas.regular.ttf"))
        const val TEXT_MARGIN_ALPHA = 0.2f
    }

    private val textExclusions = mutableListOf<TextExclusion>()

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        val segment = Line2D.Double(from.x, from.y, to.x, to.y)

        val inner = segment.stroke(stroke.width)
        drawLinePart(inner, stroke.color)

        if (outline != null) {
            val ring = segment.stroke(stroke.width + outline.width).apply {
                subtract(inner)
            }

            drawLinePart(ring, outline.color)
        }
    }

    override fun drawPolygon(shape: Polygon, color: Color, outline: Stroke?, borderRadius: Float) {
        val hex = shape.toShape(borderRadius)

        if (!color.isTransparent()) {
            graphics.color = color.awt
            graphics.fill(hex)
        }

        if (outline != null) {
            graphics.color = outline.color.awt
            graphics.stroke = BasicStroke(outline.width)
            graphics.draw(hex)
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
        val baseFont = when (font) {
            FontType.SansSerifBold -> OPENSANS_BOLD_FONT
            FontType.MonospaceRegular -> CONSOLAS_FONT
        }

        // Measure at the requested size.
        var currentFont = baseFont.deriveFont(fontSize)
        var layout = TextLayout(text, currentFont, graphics.fontRenderContext)

        val effectiveFontSize = fontSize * min(1.0, maxWidth / layout.bounds.width).toFloat()

        // Recreate the layout using the final font.
        currentFont = baseFont.deriveFont(effectiveFontSize)
        graphics.font = currentFont
        layout = TextLayout(text, currentFont, graphics.fontRenderContext)

        // Aligning horizontally with fm and vertically with bounds gives the best results for some reason
        val fm = graphics.fontMetrics
        val textX = point.x - fm.stringWidth(text) / 2.0
        val textY = point.y + layout.bounds.height / 2

        val shape = layout.getOutline(AffineTransform.getTranslateInstance(textX, textY))

        val margin = BasicStroke(
            effectiveFontSize / 6f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        ).createStrokedShape(shape)

        textExclusions += TextExclusion(
            shape,
            margin,
            margin.bounds2D,
        )

        graphics.color = color.awt
        graphics.fill(shape)
    }

    private fun drawLinePart(shape: Area, color: Color) {
        val visible = Area(shape)
        val margins = Area()
        val lineBounds = shape.bounds2D

        textExclusions.forEach { exclusion ->
            if (lineBounds.intersects(exclusion.bounds)) {
                visible.subtract(Area(exclusion.shape))
                margins.add(Area(exclusion.margin))
            }
        }

        val margin = Area(visible).apply { intersect(margins) }
        visible.subtract(margin)

        graphics.color = color.awt
        graphics.fill(visible)

        val composite = graphics.composite
        graphics.composite = AlphaComposite.SrcOver.derive(TEXT_MARGIN_ALPHA)

        graphics.fill(margin)
        graphics.composite = composite
    }

    private fun Line2D.Double.stroke(width: Float) = Area(
        BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(this),
    )

    private fun Polygon.toShape(borderRadius: Float): Shape {
        val path = Path2D.Double()
        val polygonPath = toPath(borderRadius)

        path.moveTo(polygonPath.start.x, polygonPath.start.y)
        polygonPath.segments.forEach { segment ->
            when (segment) {
                is PolygonPath.Segment.Line -> path.lineTo(segment.to.x, segment.to.y)
                is PolygonPath.Segment.QuadraticCurve -> {
                    path.quadTo(segment.control.x, segment.control.y, segment.to.x, segment.to.y)
                }
            }
        }

        path.closePath()
        return path
    }
}

private val Color.awt get() = AwtColor(rgba, true)
