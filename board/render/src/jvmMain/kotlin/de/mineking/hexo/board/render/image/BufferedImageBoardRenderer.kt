package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.FontType
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.render.image.theme.brighter
import de.mineking.hexo.board.render.image.theme.darker
import de.mineking.hexo.board.render.image.theme.isDark
import de.mineking.hexo.utils.cache.SizeCalculator
import de.mineking.hexo.utils.cache.SizeLimit
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.max

data class BufferedImageBoardRenderer(
    internal val layoutRadius: Double,
    internal val padding: Int,
    internal val visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
    internal val validate: ((BoardRenderLayout, Int, Int) -> Unit)? = null,
) : BoardRenderer<Theme, BufferedImage> {
    companion object {
        val Default = BufferedImageBoardRenderer(
            layoutRadius = 64.0,
            padding = 32,
        )
    }

    override suspend fun render(board: Board, param: Theme) = board.renderToImage(
        layoutRadius = layoutRadius,
        padding = padding,
        visibleRadius = visibleRadius,
        validate = validate,
        theme = param,
    )
}

fun <P> BoardRenderer<P, BufferedImage>.outputPngBytes() = object : BoardRenderer<P, ByteArray> {
    override suspend fun render(board: Board, param: P) = this@outputPngBytes.render(board, param).toPngBytes()
}

suspend fun <P> BoardRenderer<P, BufferedImage>.renderPngBytes(board: Board, param: P) = render(board, param).toPngBytes()
suspend fun BoardRenderer<Unit, BufferedImage>.renderPngBytes(board: Board) = renderPngBytes(board, Unit)

private fun BufferedImage.toPngBytes(): ByteArray = ByteArrayOutputStream().apply {
    ImageIO.write(this@toPngBytes, "png", this@apply)
}.toByteArray()

class ImageSizeLimitExceededException(
    val requiredBytes: Long,
    val limitBytes: Long,
) : RuntimeException("Image requires $requiredBytes bytes, limit is $limitBytes")

fun BufferedImageBoardRenderer.limitSize(maxSize: SizeLimit<ByteArray>): BufferedImageBoardRenderer {
    require(maxSize.calculator is SizeCalculator.Bytes)
    return copy(validate = { layout, width, height ->
        this.validate?.invoke(layout, width, height)

        val bytes = width.toLong() * height.toLong() * 4L
        if (bytes > maxSize.limit) throw ImageSizeLimitExceededException(bytes, maxSize.limit)
    })
}

data class ErrorMessage(val title: String, val details: String)

fun BufferedImageBoardRenderer.drawExceptionMessages(handler: suspend (Exception) -> ErrorMessage?) = object : BoardRenderer<Theme, BufferedImage> {
    override suspend fun render(board: Board, param: Theme): BufferedImage {
        @Suppress("TooGenericExceptionCaught")
        try {
            return this@drawExceptionMessages.render(board, param)
        } catch (e: Exception) {
            val message = handler(e) ?: throw e
            return drawMessage(message, param)
        }
    }

    private fun drawMessage(message: ErrorMessage, theme: Theme): BufferedImage {
        val layout = ErrorMessageLayout(message, layoutRadius, padding)
        val palette = ErrorMessagePalette(theme.backgroundColor)
        return BufferedImage(layout.width, layout.height, BufferedImage.TYPE_INT_ARGB).apply {
            withGraphics { graphics ->
                graphics.drawBackground(layout, palette)
                graphics.drawContent(layout, palette)
            }
        }
    }
}

private data class ErrorMessagePalette(val background: Color) {
    private val darkBackground = background.isDark()

    val card = if (darkBackground) background.brighter(0.12) else background.darker(0.08)
    val title = if (darkBackground) Color.rgb(0xfb7185) else Color.rgb(0xbe123c)
    val details = if (darkBackground) Color.rgb(0xe2e8f0) else Color.rgb(0x334155)
}

private data class TextBlock(
    val lines: List<String>,
    val font: FontType,
    val fontSize: Float,
    val lineHeight: Double,
) {
    val width = lines.maxOf { font.estimateTextWidth(it) } * fontSize
    val height = lines.size * lineHeight
}

private class ErrorMessageLayout(message: ErrorMessage, layoutRadius: Double, padding: Int) {
    val radius = layoutRadius.coerceAtLeast(1.0)
    val outerPadding = padding.coerceAtLeast(0)
    val cardPadding = radius * 0.7
    val sectionGap = radius * 0.32
    val badgeSize = radius * 0.62
    val badgeGap = radius * 0.3
    private val maximumTextWidth = radius * 8
    private val minimumTextWidth = radius * 3.5

    val title = message.title.toTextBlock(
        font = FontType.SansSerifBold,
        fontSize = (radius * 0.46).toFloat(),
        lineHeight = radius * 0.6,
    )

    val details = message.details.toTextBlock(
        font = FontType.MonospaceRegular,
        fontSize = (radius * 0.3).toFloat(),
        lineHeight = radius * 0.44,
    )

    val contentWidth = max(title.width, details.width).coerceIn(minimumTextWidth, maximumTextWidth)
    val contentHeight = badgeSize + badgeGap + title.height + sectionGap + details.height
    val cardWidth = contentWidth + cardPadding * 2
    val cardHeight = max(radius * 2.8, contentHeight + cardPadding * 2)
    val width = ceil(cardWidth + outerPadding * 2).toInt().coerceAtLeast(1)
    val height = ceil(cardHeight + outerPadding * 2).toInt().coerceAtLeast(1)
    val contentTop = (height - contentHeight) / 2.0

    private fun String.toTextBlock(font: FontType, fontSize: Float, lineHeight: Double) = TextBlock(
        lines = wrap(maximumTextWidth / fontSize, font),
        font = font,
        fontSize = fontSize,
        lineHeight = lineHeight,
    )
}

private inline fun BufferedImage.withGraphics(block: (Graphics2D) -> Unit) {
    val graphics = createGraphics()
    try {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        block(graphics)
    } finally {
        graphics.dispose()
    }
}

private fun Graphics2D.drawBackground(layout: ErrorMessageLayout, palette: ErrorMessagePalette) {
    color = palette.background.awt
    fillRect(0, 0, layout.width, layout.height)

    val cornerRadius = ceil(layout.radius * 0.45).toInt()
    color = palette.card.awt
    fillRoundRect(
        layout.outerPadding,
        layout.outerPadding,
        ceil(layout.cardWidth).toInt(),
        ceil(layout.cardHeight).toInt(),
        cornerRadius,
        cornerRadius,
    )
}

private fun Graphics2D.drawContent(layout: ErrorMessageLayout, palette: ErrorMessagePalette) {
    val centerX = layout.width / 2.0
    color = palette.title.awt
    fillOval(
        (centerX - layout.badgeSize / 2).toInt(),
        layout.contentTop.toInt(),
        ceil(layout.badgeSize).toInt(),
        ceil(layout.badgeSize).toInt(),
    )

    val backend = AwtRenderingBackend(this)
    backend.drawString(
        point = Point(centerX, layout.contentTop + layout.badgeSize / 2),
        text = "!",
        maxWidth = layout.badgeSize * 0.5,
        fontSize = (layout.badgeSize * 0.7).toFloat(),
        font = FontType.SansSerifBold,
        color = palette.card,
    )

    val titleTop = layout.contentTop + layout.badgeSize + layout.badgeGap
    backend.drawBlock(layout.title, titleTop, centerX, layout.contentWidth, palette.title)
    val detailsTop = titleTop + layout.title.height + layout.sectionGap
    backend.drawBlock(layout.details, detailsTop, centerX, layout.contentWidth, palette.details)
}

private fun AwtRenderingBackend.drawBlock(
    block: TextBlock,
    top: Double,
    centerX: Double,
    maximumWidth: Double,
    color: Color,
) = block.lines.forEachIndexed { index, line ->
    if (line.isNotEmpty()) {
        drawString(
            point = Point(centerX, top + block.lineHeight * (index + 0.5)),
            text = line,
            maxWidth = maximumWidth,
            fontSize = block.fontSize,
            font = block.font,
            color = color,
        )
    }
}

private fun String.wrap(maximumWidth: Double, font: FontType): List<String> = lineSequence()
    .flatMap { paragraph ->
        val words = paragraph.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        if (words.isEmpty()) return@flatMap sequenceOf("")

        sequence {
            var line = words.first()
            for (word in words.drop(1)) {
                val candidate = "$line $word"
                if (font.estimateTextWidth(candidate) <= maximumWidth) {
                    line = candidate
                } else {
                    yield(line)
                    line = word
                }
            }
            yield(line)
        }
    }
    .toList()
    .ifEmpty { listOf("") }
