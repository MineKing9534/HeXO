package de.mineking.hexo.discord.linkedroles

import de.mineking.hexo.discord.oauth2.OAuth2TokenRepository
import de.mineking.hexo.discord.oauth2.Scope
import de.mineking.hexo.discord.oauth2.TokenResponse
import de.mineking.hexo.discord.oauth2.api.OAuth2FlowHandler
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import io.ktor.server.application.ApplicationCall

class LinkedRolesOAuth2FlowHandler(
    private val tokenRepository: OAuth2TokenRepository,
) : OAuth2FlowHandler {
    override val flow = OAuth2Flow.LinkedRoles
    override val scopes = setOf(Scope.Identify, Scope.RoleConnectionsWrite)

    override suspend fun ApplicationCall.complete(tokens: TokenResponse) {
        tokenRepository.store(tokens.tokens, flow)
    }
}
