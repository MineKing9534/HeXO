package de.mineking.hexo.launcher.web

import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.HdsHttpClient
import de.mineking.hexo.launcher.loadConfig
import de.mineking.hexo.launcher.web.web.BuildConfig
import de.mineking.hexo.utils.types.isSuccess
import de.mineking.hexo.watchparty.client.WatchPartyClient
import de.mineking.hexo.watchparty.model.WatchPartyId
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLQueryComponent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.respondResource
import io.ktor.server.response.respondText
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

fun main() {
    val hds = HdsApiClient(HdsHttpClient.createDefault(BuildConfig.HDS_API_URL))
    val config = loadConfig<WebApplicationConfig>()

    embeddedServer(Netty, port = config.server.port) {
        install(IgnoreTrailingSlash)

        routing {
            sessionsRoute(hds)
            gamesRoute(hds)
            watchPartyRoute()

            get("/{path...}") {
                val path = call.request.path().trim('/')
                when (val resource = resolveResource(path)) {
                    null -> call.respondPage(
                        resource = "web/not-found.html",
                        metadata = OpenGraphMetadata.Default.copy(description = "Page not found!"),
                    )
                    is WebResource.Page -> call.respondPage(resource.path)
                    is WebResource.Asset -> call.respondResource(resource.path)
                }
            }
        }
    }.start(wait = true)
}

private fun Route.sessionsRoute(hds: HdsApiClient) {
    entityRoute(
        path = "sessions",
        meta = OpenGraphMetadata.Default,
    ) { id, call ->
        if (!call.isCrawler()) {
            return@entityRoute OpenGraphMetadata(
                title = "Watch Session $id",
                description = "Watch live session $id.",
            )
        }

        val session = hds.sessionRepository.getSession(SessionId(id))
        if (!session.isSuccess()) {
            return@entityRoute OpenGraphMetadata(
                title = "Session Not Found",
                description = "The requested session doesn't exist!",
            )
        }

        fun formatPlayer(index: Int): String {
            val player = session.value.players[index]
            return "${player.displayName}${player.elo?.let { " ($it ELO)" }.orEmpty()}"
        }

        val type = when {
            session.value.tournament != null -> "Tournament"
            session.value.gameOptions.rated -> "Rated"
            else -> "Casual"
        }

        val timeControl = when (val tc = session.value.gameOptions.timeControl) {
            is TimeControl.Turn -> "${tc.turnTime} turn based"
            is TimeControl.Match -> "${tc.mainTime} +${tc.increment}"
            is TimeControl.Unlimited -> "no"
        } + " time control"

        OpenGraphMetadata(
            title = "Watch $type Session $id",
            description = when (session.value.players.size) {
                0 -> "Waiting for players to join... ($timeControl)"
                1 -> "${formatPlayer(0)} is waiting for an opponent with $timeControl!"
                2 -> "Watch ${formatPlayer(0)} and ${formatPlayer(1)} compete live with $timeControl!"
                else -> error("")
            },
        )
    }
}

private fun Route.gamesRoute(hds: HdsApiClient) {
    entityRoute(
        path = "games",
        meta = OpenGraphMetadata(
            title = "Match History",
            description = "Browse finished games and review past matches.",
        ),
    ) { id, call ->
        if (!call.isCrawler()) {
            return@entityRoute OpenGraphMetadata(
                title = "Review $id",
                description = "Review finished game $id.",
            )
        }

        val game = hds.finishedGameRepository.getGame(GameId(id))
        if (!game.isSuccess()) {
            return@entityRoute OpenGraphMetadata(
                title = "Game Not Found",
                description = "The requested game doesn't exist!",
            )
        }

        @Suppress("MaximumLineLength")
        OpenGraphMetadata(
            title = "Review ${game.value.players[0].displayName} vs ${game.value.players[1].displayName}",
            description = "${game.value.result.winner?.displayName ?: "No one"} won after ${game.value.moveCount} moves in ${game.value.result.duration.inWholeSeconds.seconds}.",
            imageUrl = "${BuildConfig.HMD_API_URL}/render?type=png&notation=${"https://hexo.did.science/games/$id".encodeURLQueryComponent()}",
        )
    }
}

private fun Route.watchPartyRoute() {
    val watchPartyClient = WatchPartyClient(publicUrl = "", apiUrl = BuildConfig.HMD_API_URL)
    entityRoute(
        path = "watchparty",
        meta = OpenGraphMetadata(
            title = "Watchparties",
            description = "Host or join a watchparty to view matches together with others!",
        ),
    ) { id, call ->
        if (!call.isCrawler()) {
            return@entityRoute OpenGraphMetadata(
                title = "Join Watchparty $id",
                description = "Join this watchparty to watch sessions with others!",
            )
        }

        val watchParty = watchPartyClient.getWatchParty(WatchPartyId(id))
        if (!watchParty.isSuccess()) {
            return@entityRoute OpenGraphMetadata(
                title = "Watchparty Not Found",
                description = "The requested watchparty doesn't exist!",
            )
        }

        OpenGraphMetadata(
            title = "Join Watchparty",
            description = "Join this watchparty to spectate sessions, games and work together in the sandbox!",
        )
    }
}

private fun Route.entityRoute(path: String, meta: OpenGraphMetadata, entityMeta: suspend (String, ApplicationCall) -> OpenGraphMetadata) {
    route(path) {
        get {
            call.respondPage("web/$path/index.html", meta)
        }

        get("{id}") {
            val id = call.parameters.getOrFail("id")

            @Suppress("TooGenericExceptionCaught")
            val meta = try {
                entityMeta(id, call)
            } catch (e: Exception) {
                logger.error(e) { "Failed to assemble opengraph metadata" }
                OpenGraphMetadata.Default
            }

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

private fun ApplicationCall.isCrawler(): Boolean {
    val userAgent = request.headers[HttpHeaders.UserAgent] ?: return false
    return CRAWLER_USER_AGENT.containsMatchIn(userAgent)
}

private val CRAWLER_USER_AGENT = Regex(
    pattern = "bot|crawler|spider|facebookexternalhit|twitterbot|discordbot|slackbot|telegrambot|linkedinbot|whatsapp",
    option = RegexOption.IGNORE_CASE,
)
