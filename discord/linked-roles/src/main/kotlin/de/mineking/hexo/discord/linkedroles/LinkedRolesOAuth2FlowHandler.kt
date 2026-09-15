package de.mineking.hexo.discord.linkedroles

import de.mineking.hexo.discord.oauth2.OAuth2TokenRepository
import de.mineking.hexo.discord.oauth2.OAuth2Tokens
import de.mineking.hexo.discord.oauth2.Scope
import de.mineking.hexo.discord.oauth2.api.OAuth2FlowError
import de.mineking.hexo.discord.oauth2.api.OAuth2FlowHandler
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.utils.types.Result

class LinkedRolesOAuth2FlowHandler(
    private val tokenRepository: OAuth2TokenRepository,
) : OAuth2FlowHandler {
    override val flow = OAuth2Flow.LinkedRoles
    override val scopes = setOf(Scope.Identify, Scope.RoleConnectionsWrite)

    override suspend fun complete(tokens: OAuth2Tokens): Result<Unit, OAuth2FlowError> {
        tokenRepository.store(tokens, flow)

        return Result.Success(Unit)
    }
}
