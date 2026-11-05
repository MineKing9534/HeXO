package de.mineking.hexo.bot.utils

import de.mineking.hexo.board.recognition.BoardRecognitionResult
import de.mineking.hexo.board.recognition.BoardRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSource
import kotlinx.io.buffered
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.tree.MessageComponentTree
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.exceptions.HttpException
import net.dv8tion.jda.api.utils.AttachmentProxy
import net.dv8tion.jda.api.utils.FileProxy
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.io.UncheckedIOException
import kotlin.time.Duration.Companion.seconds

private val IMAGE_DOWNLOAD_TIMEOUT = 5.seconds
private val recognitionPermit = Semaphore(1)

internal fun Message.recognitionImages(): List<FileProxy> {
    val collector = RecognitionImageCollector()

    collector.collect(attachments, embeds, componentTree)
    messageSnapshots.forEach { collector.collect(it.attachments, it.embeds, it.componentTree) }

    return collector.images
}

private class RecognitionImageCollector {
    private val attachmentIds = mutableSetOf<Long>()
    private val imageUrls = mutableSetOf<String>()
    val images = mutableListOf<FileProxy>()

    fun collect(attachments: List<Message.Attachment>, embeds: List<MessageEmbed>, components: MessageComponentTree) {
        attachments.filter { it.isImage }.forEach {
            addImage(FileProxy(it.url), it.idLong, it.proxyUrl)
        }

        components.findAll(MediaGallery::class.java)
            .flatMap { it.items }
            .forEach {
                val media = it.resolvedMedia ?: return@forEach
                if (media.contentType?.startsWith("image/") != true) return@forEach

                addImage(media.proxy, media.attachmentIdLong, media.url)
            }

        embeds.forEach { embed ->
            embed.image?.let { image ->
                image.proxy?.let { addImage(it, null, image.url) }
            }
            embed.thumbnail?.let { thumbnail ->
                thumbnail.proxy?.let { addImage(it, null, thumbnail.url) }
            }
        }
    }

    private fun addImage(proxy: FileProxy, attachmentId: Long?, vararg aliases: String?) {
        val urls = (aliases.filterNotNull() + proxy.url).filterNot { it.startsWith("attachment://") }
        val duplicate = attachmentId in attachmentIds || urls.any { it in imageUrls }
        attachmentId?.let { attachmentIds += it }
        imageUrls += urls
        if (!duplicate) images += proxy
    }
}

// Bound the whole pipeline, including decoded images waiting for CPU processing.
internal suspend fun BoardRecognizer.recognizeImage(proxy: FileProxy): BoardRecognitionResult = recognitionPermit.withPermit {
    withTimeout(IMAGE_DOWNLOAD_TIMEOUT) {
        withContext(Dispatchers.IO) {
            // Discord's image proxy defaults to WebP, which the bundled ImageIO readers cannot decode.
            val downloadProxy = if (proxy is AttachmentProxy) {
                val url = proxy.url.toHttpUrl().newBuilder().setQueryParameter("format", "png").build()
                FileProxy(url.toString())
            } else {
                proxy
            }

            val download = downloadProxy.download()
            try {
                val image = download.await()
                withContext(Dispatchers.Default) {
                    recognizeBoard(image.asSource().buffered())
                }
            } catch (e: UncheckedIOException) {
                throw e.cause ?: IOException("Image download failed", e)
            } catch (e: HttpException) {
                throw IOException("Image download failed", e)
            } finally {
                // Close even if cancellation prevents await from delivering a completed download.
                if (download.isDone && !download.isCompletedExceptionally) {
                    download.getNow(null)?.close()
                }
            }
        }
    }
}
