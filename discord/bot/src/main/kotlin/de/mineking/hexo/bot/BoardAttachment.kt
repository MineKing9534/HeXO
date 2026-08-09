package de.mineking.hexo.bot

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.utils.FileUpload
import java.util.UUID

sealed interface BoardAttachment {
    fun toMediaGalleryItem(): MediaGalleryItem

    class Url(val url: String) : BoardAttachment {
        override fun toMediaGalleryItem() = MediaGalleryItem.fromUrl(url)
    }

    class Upload(val data: ByteArray, val extension: String) : BoardAttachment {
        private fun name() = "${UUID.randomUUID()}.$extension"
        override fun toMediaGalleryItem() = MediaGalleryItem.fromFile(FileUpload.fromData(data, name()))
    }
}

fun <P> BoardRenderer<P, ByteArray>.outputBoardAttachment(extension: String) = object : BoardRenderer<P, BoardAttachment> {
    override suspend fun render(board: Board, param: P) = BoardAttachment.Upload(this@outputBoardAttachment.render(board, param), extension)
}
