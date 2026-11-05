package de.mineking.hexo.board.render.server

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.binary.BoardBinary
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.server.api.ApiModule
import io.ktor.http.ContentType
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail

data class RenderType(val renderer: BoardRenderer<Theme, ByteArray>, val type: ContentType)

class BoardRenderApiModule(
    private val parser: NotationParser,
    private val renderers: Map<String, RenderType>,
) : ApiModule() {
    override fun Route.registerRoutes() {
        get("/render") {
            val type = call.queryParameters.getOrFail("type")
            val theme = parseRenderTheme(call.queryParameters["theme"])
            val renderer = renderers[type] ?: throw BadRequestException("Unsupported render type")

            try {
                val board = call.parseBoard()
                call.respondBytes(renderer.type) { renderer.renderer.render(board, theme.theme) }
            } catch (e: HexoNotationException) {
                throw BadRequestException(e.message, e)
            }
        }
    }

    private suspend fun RoutingCall.parseBoard(): Board {
        val notation = queryParameters["notation"]
        val encoded = queryParameters["encoded"]

        if (notation == null) {
            val encoded = encoded ?: throw MissingRequestParameterException("encoded")
            return BoardBinary.decodeFromString(encoded)
        } else {
            if (encoded != null) throw BadRequestException("'notation' and 'encoded' are mutually exclusive")
            return parser.parse(notation)
        }
    }
}

internal fun parseRenderTheme(value: String?): DefaultTheme {
    if (value == null) return DefaultTheme.HDS
    return DefaultTheme.entries.firstOrNull { it.name == value }
        ?: throw BadRequestException("Unsupported render theme")
}
