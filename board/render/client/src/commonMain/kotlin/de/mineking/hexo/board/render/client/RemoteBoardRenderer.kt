package de.mineking.hexo.board.render.client

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.binary.BoardBinary
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readBuffer
import kotlinx.io.Source

class RemoteBoardRenderer(
    private val apiUrl: String = "https://hexo.mineking.dev/api",
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    },
) : BoardRenderer<Theme, Source> {
    override suspend fun render(board: Board, param: Theme): Source {
        val theme = DefaultTheme.entries
            .firstOrNull { it.theme == param }
            ?: throw IllegalArgumentException("Custom themes are not supported (yet)")

        val response = client.get("$apiUrl/render") {
            parameter("encoded", BoardBinary.encodeToString(board))
            parameter("theme", theme.name)
            parameter("type", "png")
        }

        check(response.status.isSuccess()) { response.bodyAsText() }
        return response.bodyAsChannel().readBuffer()
    }
}
