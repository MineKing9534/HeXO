package de.mineking.hexo.board.recognition.server

import de.mineking.hexo.board.recognition.BoardRecognitionException
import de.mineking.hexo.board.recognition.BoardRecognitionImageTooLargeException
import de.mineking.hexo.board.recognition.BoardRecognitionOptions
import de.mineking.hexo.board.recognition.BoardRecognizer
import de.mineking.hexo.server.api.ApiModule
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

class BoardRecognizerApiModule(
    private val delegate: BoardRecognizer,
    private val maxImageBytes: Int = 16 * 1024 * 1024,
) : ApiModule() {
    init {
        require(maxImageBytes > 0)
    }

    override fun Route.registerRoutes() {
        post("/recognize") {
            val options = try {
                call.queryParameters["options"]?.let { Json.decodeFromString<BoardRecognitionOptions>(it) }
                    ?: BoardRecognitionOptions.Default
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("Invalid recognition options", e)
            }

            val channel = call.receiveChannel()
            val limit = maxImageBytes.toLong() + 1
            val result = channel.readRemaining(limit).use { image ->
                if (image.request(limit)) {
                    channel.cancel(null)
                    call.respondText("Image too large", status = HttpStatusCode.PayloadTooLarge)
                    return@post
                }
                if (image.exhausted()) throw BadRequestException("Image is required")

                try {
                    withContext(Dispatchers.Default) {
                        delegate.recognizeBoard(image, options)
                    }
                } catch (_: BoardRecognitionImageTooLargeException) {
                    call.respondText("Image too large", status = HttpStatusCode.PayloadTooLarge)
                    return@post
                } catch (e: BoardRecognitionException) {
                    call.respondText(e.message ?: "Board recognition failed", status = HttpStatusCode.UnprocessableEntity)
                    return@post
                } catch (e: IOException) {
                    throw BadRequestException("Invalid or unsupported image", e)
                }
            }
            call.respond(result)
        }
    }
}
