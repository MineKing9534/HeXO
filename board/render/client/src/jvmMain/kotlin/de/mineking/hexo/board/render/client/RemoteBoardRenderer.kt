package de.mineking.hexo.board.render.client

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.Theme
import io.ktor.utils.io.streams.inputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun RemoteBoardRenderer.outputBufferedImage() = object : BoardRenderer<Theme, BufferedImage> {
    override suspend fun render(board: Board, param: Theme): BufferedImage {
        val data = this@outputBufferedImage.render(board, param)
        return withContext(Dispatchers.IO) {
            ImageIO.read(data.inputStream())
        }
    }
}
