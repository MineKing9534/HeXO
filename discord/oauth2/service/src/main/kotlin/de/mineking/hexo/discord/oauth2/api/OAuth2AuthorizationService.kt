package de.mineking.hexo.discord.oauth2.api

import de.mineking.hexo.discord.oauth2.DiscordOAuth2Client
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result

sealed interface OAuth2AuthorizationError : IError

data object UnsupportedOAuth2Flow : OAuth2AuthorizationError
data object InvalidOAuth2AuthorizationState : OAuth2AuthorizationError
data class OAuth2CodeExchangeFailed(val flow: OAuth2Flow) : OAuth2AuthorizationError
data class OAuth2FlowCompletionFailed(val flow: OAuth2Flow, val cause: OAuth2FlowError) : OAuth2AuthorizationError

class OAuth2AuthorizationService(
    private val discordOAuth2Client: DiscordOAuth2Client,
    private val sessionStore: OAuth2AuthorizationSessionStore,
    handlers: Collection<OAuth2FlowHandler>,
) {
    private val handlers = handlers.associateBy(OAuth2FlowHandler::flow)

    init {
        require(this.handlers.size == handlers.size) { "Only one handler may be registered for each OAuth2 flow" }
    }

    suspend fun createAuthorization(flow: OAuth2Flow): Result<String, UnsupportedOAuth2Flow> {
        val handler = handlers[flow] ?: return Result.Error(UnsupportedOAuth2Flow)
        val state = sessionStore.create(flow)
        val url = discordOAuth2Client.generateAuthorizationUrl(handler.scopes, state = state)

        return Result.Success(url)
    }

    suspend fun completeAuthorization(code: String, state: String): Result<OAuth2Flow, OAuth2AuthorizationError> {
        val session = sessionStore.consume(state) ?: return Result.Error(InvalidOAuth2AuthorizationState)
        val handler = handlers[session.flow] ?: return Result.Error(UnsupportedOAuth2Flow)
        val tokens = when (val result = discordOAuth2Client.exchangeAuthorizationCode(code)) {
            is Result.Success -> result.value
            is Result.Error -> return Result.Error(OAuth2CodeExchangeFailed(session.flow))
        }

        return when (val result = handler.complete(tokens)) {
            is Result.Success -> Result.Success(session.flow)
            is Result.Error -> Result.Error(OAuth2FlowCompletionFailed(session.flow, result.error))
        }
    }
}
