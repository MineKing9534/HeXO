package de.mineking.hexo.discord.oauth2.api

import de.mineking.hexo.discord.oauth2.DiscordOAuth2Client
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed class OAuth2AuthorizationError(open val flow: OAuth2Flow?) : IError

data object UnsupportedOAuth2Flow : OAuth2AuthorizationError(null)
data object InvalidOAuth2AuthorizationState : OAuth2AuthorizationError(null)
data class OAuth2CodeExchangeFailed(override val flow: OAuth2Flow) : OAuth2AuthorizationError(flow)

class OAuth2AuthorizationService(
    private val discordOAuth2Client: DiscordOAuth2Client,
    private val sessionStore: OAuth2AuthorizationSessionStore,
    handlers: Collection<OAuth2FlowHandler>,
    maxConcurrentTokenExchanges: Int = 8,
) {
    private val handlers = handlers.associateBy(OAuth2FlowHandler::flow)
    private val tokenExchangeSemaphore = Semaphore(maxConcurrentTokenExchanges)

    init {
        require(this.handlers.size == handlers.size) { "Only one handler may be registered for each OAuth2 flow" }
        require(maxConcurrentTokenExchanges > 0) { "maxConcurrentTokenExchanges must be positive" }
    }

    suspend fun createAuthorization(flow: OAuth2Flow, browserId: String): Result<String, UnsupportedOAuth2Flow> {
        val handler = handlers[flow] ?: return Result.Error(UnsupportedOAuth2Flow)
        val state = sessionStore.create(flow, browserId)
        val url = discordOAuth2Client.generateAuthorizationUrl(handler.scopes, state = state)

        return Result.Success(url)
    }

    suspend fun completeAuthorization(
        call: ApplicationCall,
        code: String,
        state: String,
        browserId: String,
    ): Result<OAuth2Flow, OAuth2AuthorizationError> {
        val session = sessionStore.consume(state, browserId) ?: return Result.Error(InvalidOAuth2AuthorizationState)
        val handler = handlers[session.flow] ?: return Result.Error(UnsupportedOAuth2Flow)
        val tokens = when (val result = tokenExchangeSemaphore.withPermit { discordOAuth2Client.exchangeAuthorizationCode(code) }) {
            is Result.Success -> result.value
            is Result.Error -> return Result.Error(OAuth2CodeExchangeFailed(session.flow))
        }

        handler.run { call.complete(tokens) }
        return Result.Success(session.flow)
    }
}
