package de.mineking.hexo.launcher.api.modules

import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.server.api.ApiModule
import io.ktor.http.ContentType
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail

data class RenderType(val renderer: BoardRenderer<Theme, ByteArray>, val type: ContentType)

class RenderApiModule(
    private val parser: BoardParser,
    private val renderers: Map<String, RenderType>,
) : ApiModule() {
    override fun Route.registerRoutes() {
        get("/render") {
            val type = call.queryParameters.getOrFail("type")
            val theme = call.queryParameters["theme"]?.let { DefaultTheme.valueOf(it) } ?: DefaultTheme.HDS
            val renderer = renderers[type] ?: throw BadRequestException("Unsupported render type")

            val notation = call.queryParameters.getOrFail("notation")
            val board = parser.parse(notation.replace("_", "/"))

            call.respondBytes(renderer.type) { renderer.renderer.render(board, theme.theme) }
        }
    }
}
