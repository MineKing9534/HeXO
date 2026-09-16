package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.Theme
import dev.jamesyox.svg4k.attr.attrs.by
import dev.jamesyox.svg4k.attr.attrs.max
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

data class BufferedImageBoardRenderer(
    private val layoutRadius: Double,
    private val padding: Int,
    private val visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
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

@JvmInline
value class Size(val bytes: Long)

val Int.megabytes get() = Size(this * 1024 * 1024L)

fun BufferedImageBoardRenderer.limitSize(maxSize: Size) = copy(validate = { layout, width, height ->
    this.validate?.invoke(layout, width, height)

    val bytes = width.toLong() * height.toLong() * 4L
    require(bytes <= maxSize.bytes) { "Image requires $bytes bytes, limit is ${maxSize.bytes}" }
})

fun <P> BoardRenderer<P, BufferedImage>.outputPngBytes() = object : BoardRenderer<P, ByteArray> {
    override suspend fun render(board: Board, param: P) = this@outputPngBytes.render(board, param).toPngBytes()
}

suspend fun <P> BoardRenderer<P, BufferedImage>.renderPngBytes(board: Board, param: P) = render(board, param).toPngBytes()
suspend fun BoardRenderer<Unit, BufferedImage>.renderPngBytes(board: Board) = renderPngBytes(board, Unit)

private fun BufferedImage.toPngBytes(): ByteArray = ByteArrayOutputStream().apply {
    ImageIO.write(this@toPngBytes, "png", this@apply)
}.toByteArray()
