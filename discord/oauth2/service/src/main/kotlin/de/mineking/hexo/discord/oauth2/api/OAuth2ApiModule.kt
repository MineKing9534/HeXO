package de.mineking.hexo.discord.oauth2.api

import de.mineking.hexo.discord.oauth2.model.OAuth2AuthorizationResponse
import de.mineking.hexo.discord.oauth2.model.OAuth2CallbackRequest
import de.mineking.hexo.discord.oauth2.model.OAuth2CallbackResponse
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.server.api.ApiModule
import de.mineking.hexo.server.api.clientAddress
import de.mineking.hexo.server.api.preventCaching
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ratelimit.RateLimitConfig
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.util.getOrFail
import java.security.SecureRandom
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val OAUTH_AUTHORIZATION_CLIENT_LIMIT = RateLimitName("oauth-authorization-client")
private val OAUTH_AUTHORIZATION_GLOBAL_LIMIT = RateLimitName("oauth-authorization-global")
private val OAUTH_CALLBACK_CLIENT_LIMIT = RateLimitName("oauth-callback-client")
private val OAUTH_CALLBACK_GLOBAL_LIMIT = RateLimitName("oauth-callback-global")
private const val OAUTH_BROWSER_COOKIE = "__Secure-hexo_oauth_browser"

class OAuth2ApiModule(
    private val authorizationService: OAuth2AuthorizationService,
    private val cookiePath: String,
    private val authorizationTtl: Duration,
) : ApiModule() {
    init {
        require(cookiePath.startsWith('/')) { "Cookie path must be absolute" }
    }

    private val secureRandom = SecureRandom()
    private fun createBrowserId() = ByteArray(32).also(secureRandom::nextBytes).toHexString()

    override fun RateLimitConfig.configureRateLimit() {
        register(OAUTH_AUTHORIZATION_CLIENT_LIMIT) {
            rateLimiter(limit = 20, refillPeriod = 1.minutes)
            requestKey { it.clientAddress() }
        }
        register(OAUTH_AUTHORIZATION_GLOBAL_LIMIT) {
            rateLimiter(limit = 300, refillPeriod = 1.minutes)
        }

        register(OAUTH_CALLBACK_CLIENT_LIMIT) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { it.clientAddress() }
        }
        register(OAUTH_CALLBACK_GLOBAL_LIMIT) {
            rateLimiter(limit = 120, refillPeriod = 1.minutes)
        }
    }

    override fun Route.registerRoutes() {
        rateLimit(OAUTH_AUTHORIZATION_GLOBAL_LIMIT) {
            rateLimit(OAUTH_AUTHORIZATION_CLIENT_LIMIT) {
                get("/oauth2/authorization") {
                    call.preventCaching()

                    val flow = OAuth2Flow.of(call.queryParameters.getOrFail("flow"))
                        ?: throw BadRequestException("Unsupported OAuth2 flow")

                    val browserId = call.request.cookies[OAUTH_BROWSER_COOKIE] ?: createBrowserId()
                    val result = authorizationService.createAuthorization(flow, browserId)
                    if (!result.isSuccess()) throw BadRequestException("OAuth2 flow is not configured")

                    call.response.cookies.append(
                        Cookie(
                            name = OAUTH_BROWSER_COOKIE,
                            value = browserId,
                            path = cookiePath,
                            secure = true,
                            httpOnly = true,
                            maxAge = authorizationTtl.inWholeSeconds.toInt(),
                            extensions = mapOf("SameSite" to "Strict"),
                        ),
                    )
                    call.respond(OAuth2AuthorizationResponse(result.value))
                }
            }
        }

        rateLimit(OAUTH_CALLBACK_GLOBAL_LIMIT) {
            rateLimit(OAUTH_CALLBACK_CLIENT_LIMIT) {
                post("/oauth2/callback") {
                    call.preventCaching()

                    val request = call.receive<OAuth2CallbackRequest>()
                    val result = call.request.cookies[OAUTH_BROWSER_COOKIE]
                        ?.let { authorizationService.completeAuthorization(call, request.code, request.state, it) }
                        ?: Result.Error(InvalidOAuth2AuthorizationState)

                    when (result) {
                        is Result.Success -> call.respond(OAuth2CallbackResponse(success = true, flow = result.value))
                        is Result.Error -> call.respond(
                            status = HttpStatusCode.Unauthorized,
                            message = OAuth2CallbackResponse(success = false, flow = result.error.flow),
                        )
                    }
                }
            }
        }
    }
}
