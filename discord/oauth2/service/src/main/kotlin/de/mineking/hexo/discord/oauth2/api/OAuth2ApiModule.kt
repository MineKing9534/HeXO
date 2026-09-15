package de.mineking.hexo.discord.oauth2.api

import de.mineking.hexo.discord.oauth2.model.OAuth2AuthorizationResponse
import de.mineking.hexo.discord.oauth2.model.OAuth2CallbackResponse
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.server.api.ApiModule
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail

class OAuth2ApiModule(
    private val authorizationService: OAuth2AuthorizationService,
) : ApiModule() {
    override fun Route.registerRoutes() {
        get("/oauth2/authorization") {
            val flow = OAuth2Flow.of(call.queryParameters.getOrFail("flow"))
                ?: throw BadRequestException("Unsupported OAuth2 flow")

            val result = authorizationService.createAuthorization(flow)
            if (!result.isSuccess()) throw BadRequestException("OAuth2 flow is not configured")

            call.respond(OAuth2AuthorizationResponse(result.value))
        }

        get("/oauth2/callback") {
            val code = call.queryParameters.getOrFail("code")
            val state = call.queryParameters.getOrFail("state")

            when (val result = authorizationService.completeAuthorization(code, state)) {
                is Result.Success -> call.respond(OAuth2CallbackResponse(success = true, flow = result.value))
                is Result.Error -> {
                    val flow = when (val error = result.error) {
                        is OAuth2CodeExchangeFailed -> error.flow
                        is OAuth2FlowCompletionFailed -> error.flow
                        else -> null
                    }
                    call.respond(HttpStatusCode.Unauthorized, OAuth2CallbackResponse(success = false, flow = flow))
                }
            }
        }
    }
}
