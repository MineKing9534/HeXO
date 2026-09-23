package de.mineking.hexo.game.implementation.service.auth

import de.mineking.hexo.discord.oauth2.Scope
import de.mineking.hexo.discord.oauth2.TokenResponse
import de.mineking.hexo.discord.oauth2.api.OAuth2FlowHandler
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import io.ktor.server.application.ApplicationCall

class LoginOAuth2FlowHandler(private val sessionManager: AuthSessionManager) : OAuth2FlowHandler {
    override val flow = OAuth2Flow.WebLogin
    override val scopes = setOf(Scope.Identify)

    override suspend fun ApplicationCall.complete(tokens: TokenResponse) {
        val session = sessionManager.createSession(tokens)
        setSessionCookies(sessionManager, session)
    }
}
