package de.mineking.hexo.discord.oauth2.api

import de.mineking.hexo.discord.oauth2.Scope
import de.mineking.hexo.discord.oauth2.TokenResponse
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import io.ktor.server.application.ApplicationCall

interface OAuth2FlowHandler {
    val flow: OAuth2Flow
    val scopes: Set<Scope>

    suspend fun ApplicationCall.complete(tokens: TokenResponse)
}
