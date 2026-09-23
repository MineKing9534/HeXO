package de.mineking.hexo.launcher.api

import com.auth0.jwt.algorithms.Algorithm
import de.mineking.hexo.board.render.image.ErrorMessage
import de.mineking.hexo.discord.linkedroles.LinkedRolesOAuth2FlowHandler
import de.mineking.hexo.discord.oauth2.api.InMemoryOAuth2AuthorizationSessionStore
import de.mineking.hexo.discord.oauth2.api.OAuth2ApiModule
import de.mineking.hexo.discord.oauth2.api.OAuth2AuthorizationService
import de.mineking.hexo.game.implementation.service.GameApiModule
import de.mineking.hexo.game.implementation.service.auth.AuthSessionManager
import de.mineking.hexo.game.implementation.service.auth.LoginOAuth2FlowHandler
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.HdsHttpClient
import de.mineking.hexo.launcher.CachingRepositoryWrapper
import de.mineking.hexo.launcher.api.modules.RenderApiModule
import de.mineking.hexo.launcher.api.modules.RenderType
import de.mineking.hexo.launcher.createBoardParser
import de.mineking.hexo.launcher.createBoardRenderer
import de.mineking.hexo.launcher.createDatabase
import de.mineking.hexo.launcher.createOAuth2Dependencies
import de.mineking.hexo.launcher.formatBytes
import de.mineking.hexo.launcher.loadConfig
import de.mineking.hexo.launcher.shutdownHook
import de.mineking.hexo.server.HttpServer
import de.mineking.hexo.server.api.ApiModule
import de.mineking.hexo.watchparty.server.WatchPartyApiModule
import io.ktor.http.ContentType
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.minutes

suspend fun main() {
    val config = loadConfig<ApiApplicationConfig>()
    val database = config.database.createDatabase()

    val hds = HdsApiClient(
        client = HdsHttpClient.createDefault(),
        repositoryWrapper = CachingRepositoryWrapper,
    )

    val (gameModule, loginHandler) = config.auth?.let { config ->
        if (database == null) return@let null

        val authManager = AuthSessionManager(
            database = database,
            algorithm = Algorithm.HMAC256(Base64.decode(config.secret)),
            accessTokenTtl = config.accessTokenTtl,
            refreshTokenTtl = config.refreshTokenTtl,
        )
        val gameModule = GameApiModule(database, authManager)
        val loginHandler = LoginOAuth2FlowHandler(authManager)

        gameModule to loginHandler
    } ?: (null to null)

    val parser = createBoardParser(hds)
    val renderer = createBoardRenderer()

    val oauth2 = createOAuth2Dependencies(config.oauth2, config.server.url, database)
    val oauth2Module = oauth2?.let {
        OAuth2ApiModule(
            OAuth2AuthorizationService(
                discordOAuth2Client = it.client,
                sessionStore = InMemoryOAuth2AuthorizationSessionStore(
                    expireAfter = 5.minutes,
                    maxEntries = 100,
                ),
                handlers = listOfNotNull(LinkedRolesOAuth2FlowHandler(it.tokenRepository), loginHandler),
            ),
        )
    }
    val server = HttpServer(
        modules = listOfNotNull(
            HealthApiModule(),
            oauth2Module,
            gameModule,
            WatchPartyApiModule(),
            RenderApiModule(parser, mapOf("png" to RenderType(renderer, ContentType.Image.PNG))),
        ),
        port = config.server.port,
    )

    shutdownHook {
        server.stop()
        oauth2?.close()
        database?.close()
    }

    printBanner()

    server.start(wait = true)
}

@Suppress("MaximumLineLength")
private fun createBoardRenderer() = createBoardRenderer { error ->
    ErrorMessage(
        title = "Image Too Large",
        details = "The requested image requires ${error.requiredBytes.formatBytes()} of memory to render, but the limit is ${error.limitBytes.formatBytes()}!",
    )
}

private fun printBanner() {
    println("""
     _    _     __   ______                _____ _____          ___  
    | |  | |    \ \ / / __ \         /\   |  __ \_   _|        |__ \ 
    | |__| | ___ \ V / |  | | ___   /  \  | |__) || |     __   __ ) |
    |  __  |/ _ \ > <| |  | ||___| / /\ \ |  ___/ | |     \ \ / // / 
    | |  | |  __// . \ |__| |     / ____ \| |    _| |_     \ V // /_ 
    |_|  |_|\___/_/ \_\____/     /_/    \_\_|   |_____|     \_/|____|
    """)
}

private class HealthApiModule : ApiModule() {
    override fun Route.registerRoutes() {
        get("/health") {
            call.respond(HealthResponse("Ok"))
        }
    }
}

@Serializable
private class HealthResponse(val status: String)
