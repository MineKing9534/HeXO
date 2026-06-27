package de.mineking.hexo.bot.web

import de.mineking.hexo.link.oauth2.DiscordOAuth2Client
import de.mineking.hexo.link.oauth2.Scope
import de.mineking.hexo.sever.service.WebService
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

class LinkedRolesRedirectWebService(
    private val stateGenerator: () -> String,
    private val discordOAuth2Client: DiscordOAuth2Client,
) : WebService() {
    override fun Route.registerRoutes() {
        get("/linked-roles") {
            val url = discordOAuth2Client.generateAuthorizationUrl(
                scopes = arrayOf(Scope.Identify, Scope.RoleConnectionsWrite),
                state = stateGenerator(),
            )
            call.respondRedirect(url)
        }
    }
}
