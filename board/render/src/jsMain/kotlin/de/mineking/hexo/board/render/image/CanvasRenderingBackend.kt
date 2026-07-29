package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.render.image.theme.isTransparent
import kotlinx.browser.document
import org.w3c.dom.ALPHABETIC
import org.w3c.dom.BUTT
import org.w3c.dom.CanvasLineCap
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.LEFT
import org.w3c.dom.MITER
import org.w3c.dom.ROUND
import kotlin.math.min

fun HTMLCanvasElement.drawBoard(
    layout: BoardRenderLayout,
    padding: Int,
    offset: Point = Point.Zero,
    theme: Theme = Theme.Default,
    renderingHook: BoardRenderingHook? = null,
) {
    val context = getContext("2d") as CanvasRenderingContext2D

    context.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    context.fillStyle = theme.backgroundColor.css
    context.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
    context.translate(padding.toDouble() + offset.x, padding.toDouble() + offset.y)

    CanvasRenderingBackend(context).drawBoard(layout, theme, renderingHook)
}

class CanvasRenderingBackend(val canvas: CanvasRenderingContext2D) : RenderingBackend {
    private data class TextExclusion(
        val text: String,
        val font: String,
        val x: Double,
        val y: Double,
        val margin: Double,
        val minX: Double,
        val minY: Double,
        val maxX: Double,
        val maxY: Double,
    )

    private val textExclusions = mutableListOf<TextExclusion>()
    private val lineLayer by lazy { createLineLayer() }

    override fun drawLine(from: Point, to: Point, stroke: Stroke, outline: Stroke?) {
        val layer = lineLayer.apply { clear() }
        val lineRadius = ((stroke.width + (outline?.width ?: 0f)) / 2.0)
        val minX = minOf(from.x, to.x) - lineRadius
        val minY = minOf(from.y, to.y) - lineRadius
        val maxX = maxOf(from.x, to.x) + lineRadius
        val maxY = maxOf(from.y, to.y) + lineRadius

        if (outline != null) {
            layer.context.strokeStyle = outline.color.css
            layer.context.lineWidth = (stroke.width + outline.width).toDouble()
            layer.context.lineCap = CanvasLineCap.ROUND
            layer.context.lineJoin = CanvasLineJoin.ROUND
            layer.context.strokeSegment(from, to)

            layer.context.save()
            layer.context.globalCompositeOperation = "destination-out"
            layer.context.lineWidth = stroke.width.toDouble()
            layer.context.strokeSegment(from, to)
            layer.context.restore()
        }

        layer.context.strokeStyle = stroke.color.css
        layer.context.lineWidth = stroke.width.toDouble()
        layer.context.lineCap = CanvasLineCap.ROUND
        layer.context.lineJoin = CanvasLineJoin.ROUND
        layer.context.strokeSegment(from, to)

        layer.context.cutOutTextExclusions(minX, minY, maxX, maxY)

        canvas.save()
        canvas.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        canvas.drawImage(layer.canvas, 0.0, 0.0)
        canvas.restore()
    }

    override fun drawPolygon(shape: Polygon, color: Color, outline: Stroke?, borderRadius: Float) {
        canvas.beginPath()
        shape.trace(borderRadius)
        canvas.closePath()

        if (!color.isTransparent()) {
            canvas.fillStyle = color.css
            canvas.fill()
        }

        if (outline != null) {
            canvas.strokeStyle = outline.color.css
            canvas.lineWidth = outline.width.toDouble()
            canvas.lineCap = CanvasLineCap.BUTT
            canvas.lineJoin = CanvasLineJoin.MITER
            canvas.stroke()
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
        fun fontAt(size: Float) = when (font) {
            FontType.SansSerifBold -> "800 ${size}px system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif"
            FontType.MonospaceRegular -> "normal ${size}px consolas, monospace, sans-serif"
        }

        canvas.font = fontAt(fontSize)
        canvas.textAlign = CanvasTextAlign.LEFT
        canvas.textBaseline = CanvasTextBaseline.ALPHABETIC

        val requestedMetrics = canvas.measureText(text)
        val effectiveFontSize = fontSize * min(1.0, maxWidth / requestedMetrics.width).toFloat()

        canvas.font = fontAt(effectiveFontSize)
        val metrics = canvas.measureText(text)
        val ascent = metrics.fontBoundingBoxAscent
        val descent = metrics.fontBoundingBoxDescent
        val textX = point.x - metrics.width / 2.0
        val textY = point.y + (ascent - descent) / 2.0
        val margin = effectiveFontSize / 6.0

        textExclusions += TextExclusion(
            text = text,
            font = canvas.font,
            x = textX,
            y = textY,
            margin = margin,
            minX = textX - margin,
            minY = textY - ascent - margin,
            maxX = textX + metrics.width + margin,
            maxY = textY + descent + margin,
        )

        canvas.fillStyle = color.css
        canvas.fillText(text, textX, textY)
    }

    private fun CanvasRenderingContext2D.strokeSegment(from: Point, to: Point) {
        beginPath()
        moveTo(from.x, from.y)
        lineTo(to.x, to.y)
        stroke()
    }

    private fun createLineLayer(): LineLayer {
        val layerCanvas = document.createElement("canvas") as HTMLCanvasElement
        layerCanvas.width = canvas.canvas.width
        layerCanvas.height = canvas.canvas.height

        val layerContext = layerCanvas.getContext("2d") as CanvasRenderingContext2D
        layerContext.setTransform(canvas.getTransform())

        return LineLayer(layerCanvas, layerContext)
    }

    private fun CanvasRenderingContext2D.cutOutTextExclusions(
        minX: Double,
        minY: Double,
        maxX: Double,
        maxY: Double,
    ) {
        if (textExclusions.isEmpty()) return

        save()
        globalCompositeOperation = "destination-out"

        textAlign = CanvasTextAlign.LEFT
        textBaseline = CanvasTextBaseline.ALPHABETIC
        lineCap = CanvasLineCap.ROUND
        lineJoin = CanvasLineJoin.ROUND

        textExclusions.forEach {
            if (it.maxX < minX || it.minX > maxX || it.maxY < minY || it.minY > maxY) return@forEach

            font = it.font
            lineWidth = it.margin
            strokeText(it.text, it.x, it.y)
            fillText(it.text, it.x, it.y)
        }

        restore()
    }

    private fun Polygon.trace(borderRadius: Float) {
        val path = toPath(borderRadius)

        canvas.moveTo(path.start.x, path.start.y)
        path.segments.forEach { segment ->
            when (segment) {
                is PolygonPath.Segment.Line -> canvas.lineTo(segment.to.x, segment.to.y)
                is PolygonPath.Segment.QuadraticCurve -> {
                    canvas.quadraticCurveTo(segment.control.x, segment.control.y, segment.to.x, segment.to.y)
                }
            }
        }
    }

    private data class LineLayer(val canvas: HTMLCanvasElement, val context: CanvasRenderingContext2D) {
        fun clear() {
            context.save()
            context.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            context.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
            context.restore()
        }
    }
}

val Color.css: String get() = "rgba(${red.toInt()}, ${green.toInt()}, ${blue.toInt()}, ${alpha.toInt() / 255.0})"
