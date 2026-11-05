package de.mineking.board.recognizer.client

import de.mineking.hexo.board.recognition.BoardRecognitionException
import de.mineking.hexo.board.recognition.BoardRecognitionOptions
import de.mineking.hexo.board.recognition.BoardRecognitionResult
import de.mineking.hexo.board.recognition.BoardRecognizer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.io.Source
import kotlinx.serialization.json.Json

class RemoteBoardRecognizer(
    private val apiUrl: String = "https://hexo.mineking.dev/api",
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    },
) : BoardRecognizer {
    override suspend fun recognizeBoard(
        image: Source,
        options: BoardRecognitionOptions,
    ): BoardRecognitionResult {
        val response = client.post("${apiUrl.trimEnd('/')}/recognize") {
            expectSuccess = false
            parameter("options", Json.encodeToString(options))
            contentType(ContentType.Application.OctetStream)
            setBody(ByteReadChannel(image))
        }

        if (response.status == HttpStatusCode.UnprocessableEntity) {
            throw BoardRecognitionException(response.bodyAsText())
        }
        check(response.status.isSuccess()) { "Board recognition request failed: ${response.status}" }

        return response.body()
    }
}
