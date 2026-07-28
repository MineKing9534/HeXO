package de.mineking.hexo.board.render.image

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.Theme
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class BufferedImageBoardRenderer(
    private val layoutRadius: Double,
    private val padding: Int,
    private val visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
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
