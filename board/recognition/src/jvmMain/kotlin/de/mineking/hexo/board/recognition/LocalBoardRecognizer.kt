package de.mineking.hexo.board.recognition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.asInputStream
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream

class LocalBoardRecognizer(
    private val maxImageBytes: Long,
    private val maxImagePixels: Long,
) : BoardRecognizer {
    companion object {
        val Default = LocalBoardRecognizer(
            maxImageBytes = 16 * 1024 * 1024,
            maxImagePixels = 16_000_000L,
        )
    }

    override suspend fun recognizeBoard(
        image: Source,
        options: BoardRecognitionOptions,
    ): BoardRecognitionResult {
        val image = withContext(Dispatchers.IO) { readImage(image) }
        return image.recognizeBoard(options)
    }

    private fun readImage(input: Source): BufferedImage {
        val buffer = Buffer()

        while (buffer.size <= maxImageBytes) {
            val remaining = maxImageBytes + 1 - buffer.size
            if (input.readAtMostTo(buffer, remaining) == -1L) break
        }

        if (buffer.size > maxImageBytes) throw BoardRecognitionImageTooLargeException()

        MemoryCacheImageInputStream(buffer.asInputStream()).use { source ->
            val readers = ImageIO.getImageReaders(source)
            if (!readers.hasNext()) throw IOException("Unsupported image format")
            val reader = readers.next()

            try {
                reader.setInput(source, true, true)

                val width = reader.getWidth(0)
                val height = reader.getHeight(0)

                if (width.toLong() * height > maxImagePixels) {
                    throw BoardRecognitionImageTooLargeException()
                }

                return reader.read(0)
            } finally {
                reader.dispose()
            }
        }
    }
}
