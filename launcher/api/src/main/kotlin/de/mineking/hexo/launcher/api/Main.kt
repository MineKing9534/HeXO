package de.mineking.hexo.launcher.api

import de.mineking.hexo.discord.linkedroles.LinkedRolesOAuth2FlowHandler
import de.mineking.hexo.discord.oauth2.api.InMemoryOAuth2AuthorizationSessionStore
import de.mineking.hexo.discord.oauth2.api.OAuth2ApiModule
import de.mineking.hexo.discord.oauth2.api.OAuth2AuthorizationService
import de.mineking.hexo.launcher.createDatabase
import de.mineking.hexo.launcher.createOAuth2Dependencies
import de.mineking.hexo.launcher.loadConfig
import de.mineking.hexo.launcher.shutdownHook
import de.mineking.hexo.server.HttpServer
import de.mineking.hexo.server.api.ApiModule
import de.mineking.hexo.watchparty.server.WatchPartyApiModule
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

suspend fun main() {
    val config = loadConfig<ApiApplicationConfig>()
    val database = config.database.createDatabase()

    val oauth2 = createOAuth2Dependencies(config.oauth2, config.server.url, database)
    val oauth2Module = oauth2?.let {
        OAuth2ApiModule(
            OAuth2AuthorizationService(
                discordOAuth2Client = it.client,
                sessionStore = InMemoryOAuth2AuthorizationSessionStore(),
                handlers = listOf(LinkedRolesOAuth2FlowHandler(it.tokenRepository)),
            ),
        )
    }
    val server = HttpServer(
        modules = listOfNotNull(HealthApiModule(), oauth2Module, WatchPartyApiModule()),
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

private fun printBanner() {
    println("""
     _    _     __   ______               _____ _____          ___  
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
            call.respond(HealthResponse())
        }
    }
}

@Serializable
private class HealthResponse(val status: String = "ok")
