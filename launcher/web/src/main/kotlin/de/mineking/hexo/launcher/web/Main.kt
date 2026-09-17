package de.mineking.hexo.launcher.web

import de.mineking.hexo.launcher.loadConfig
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.respondResource
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail

fun main() {
    val config = loadConfig<WebApplicationConfig>()
    embeddedServer(Netty, port = config.server.port) {
        routing {
            entityRoute("sessions") { id, _ ->
                OpenGraphMetadata.Default // TODO
            }
            entityRoute("games") { id, _ ->
                OpenGraphMetadata.Default // TODO
            }
            entityRoute("watchparty") { id, _ ->
                OpenGraphMetadata.Default // TODO
            }

            get("/{path...}") {
                val path = call.request.path().trim('/')
                when (val resource = resolveResource(path)) {
                    null -> call.respondPage(
                        resource = "web/not-found.html",
                        metadata = OpenGraphMetadata.Default.copy(description = "Page not found"),
                    )
                    is WebResource.Page -> call.respondPage(resource.path)
                    is WebResource.Asset -> call.respondResource(resource.path)
                }
            }
        }
    }.start(wait = true)
}

private fun Route.entityRoute(path: String, meta: (String, ApplicationCall) -> OpenGraphMetadata) {
    route(path) {
        get("/") {
            call.respondPage("web/$path/index.html")
        }

        get("{id}") {
            val id = call.parameters.getOrFail("id")
            val meta = meta(id, call)

            call.respondPage("web/$path/dynamic.html", meta)
        }
    }
}

private suspend fun ApplicationCall.respondPage(
    resource: String,
    metadata: OpenGraphMetadata = OpenGraphMetadata.Default,
) {
    val document = resourceStream(resource)?.bufferedReader()?.use { it.readText() }
    if (document == null) {
        respondText("Not found", status = HttpStatusCode.NotFound)
        return
    }

    respondText(
        text = document.injectOpenGraph(publicUrl(), metadata),
        contentType = ContentType.Text.Html,
    )
}

private fun resolveResource(path: String): WebResource? {
    if (path.split('/').any { it == ".." } || '\\' in path) return null
    if (path.endsWith(".html")) return null

    if (path.isEmpty()) return WebResource.Page("web/index.html")

    if (path.substringAfterLast('/').contains('.')) {
        return "web/$path".takeIf(::resourceExists)?.let(WebResource::Asset)
    }

    return listOf("web/$path.html", "web/$path/index.html")
        .firstOrNull(::resourceExists)
        ?.let(WebResource::Page)
}

private sealed interface WebResource {
    val path: String

    data class Page(override val path: String) : WebResource
    data class Asset(override val path: String) : WebResource
}

private fun ApplicationCall.publicUrl(): String {
    val protocol = request.headers[HttpHeaders.XForwardedProto]?.substringBefore(',')?.trim() ?: "http"
    val host = request.headers[HttpHeaders.XForwardedHost]?.substringBefore(',')?.trim()
        ?: request.headers[HttpHeaders.Host]
        ?: "localhost"
    return "$protocol://$host${request.uri}"
}

private fun resourceStream(path: String) = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
private fun resourceExists(path: String) = Thread.currentThread().contextClassLoader.getResource(path) != null
