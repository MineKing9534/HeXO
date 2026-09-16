package de.mineking.hexo.discord.oauth2

import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.requests.RestConfig
import kotlin.time.Duration.Companion.seconds

class DiscordOAuth2Client(
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String,
    private val apiUrl: String = RestConfig.DEFAULT_BASE_URL,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    },
) : AutoCloseable {
    fun generateAuthorizationUrl(scopes: Iterable<Scope>, state: String) = URLBuilder("https://discord.com/oauth2/authorize").apply {
        parameters.append("scope", scopes.joinToString(" "))
        parameters.append("client_id", clientId)
        parameters.append("redirect_uri", redirectUri)
        parameters.append("response_type", "code")
        parameters.append("prompt", "consent")

        parameters.append("state", state)
    }.buildString()

    private suspend fun getCurrentUserId(accessToken: String): DiscordUserId {
        @Serializable
        data class User(val id: DiscordUserId)

        @Serializable
        data class Response(val user: User)

        val response = retryRateLimited {
            httpClient.get("$apiUrl/oauth2/@me") {
                bearerAuth(accessToken)
            }
        }.body<Response>()

        return response.user.id
    }

    suspend fun exchangeAuthorizationCode(
        code: String,
    ): Result<OAuth2Tokens, OAuth2TokenExchangeError> {
        val response = retryRateLimited {
            httpClient.submitForm(
                "$apiUrl/oauth2/token",
                formParameters = parameters {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", redirectUri)
                },
            ) {
                basicAuth(clientId, clientSecret)
            }
        }

        if (!response.status.isSuccess()) return Result.Error(OAuth2TokenExchangeError)

        val data = response.body<OAuth2TokensDto>()
        return Result.Success(OAuth2Tokens(this, data, getCurrentUserId(data.accessToken)))
    }

    internal suspend fun refreshToken(refreshToken: String): Result<OAuth2Tokens, OAuth2TokenRefreshError> {
        return try {
            val response = retryRateLimited {
                httpClient.submitForm(
                    "$apiUrl/oauth2/token",
                    formParameters = parameters {
                        append("grant_type", "refresh_token")
                        append("refresh_token", refreshToken)
                    },
                ) {
                    basicAuth(clientId, clientSecret)
                }
            }

            when {
                response.status.isSuccess() -> {
                    val data = response.body<OAuth2TokensDto>()
                    Result.Success(OAuth2Tokens(this, data, getCurrentUserId(data.accessToken)))
                }

                response.status == HttpStatusCode.BadRequest && response.body<OAuth2ErrorResponse>().error == "invalid_grant" -> {
                    Result.Error(OAuth2TokenRefreshError.InvalidToken)
                }

                else -> Result.Error(OAuth2TokenRefreshError.TemporaryFailure)
            }
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            Result.Error(OAuth2TokenRefreshError.TemporaryFailure)
        }
    }

    internal suspend fun revokeToken(tokens: OAuth2Tokens): Boolean {
        val response = retryRateLimited {
            httpClient.submitForm(
                "$apiUrl/oauth2/token/revoke",
                formParameters = parameters {
                    append("token", tokens.data.refreshToken)
                    append("token_type_hint", "refresh_token")
                },
            ) {
                basicAuth(clientId, clientSecret)
            }
        }

        return response.status.isSuccess()
    }

    suspend fun updateLinkedRoleData(user: OAuth2Tokens, vararg values: LinkedRoleMetadataValue<*>) {
        @Serializable
        data class Request(val metadata: Map<String, String>)

        val response = retryRateLimited {
            httpClient.put("$apiUrl/users/@me/applications/$clientId/role-connection") {
                val request = Request(values.filter { it.value != null }.associate { it.key.key to it.value.toString() })
                setBody(request)
                bearerAuth(user.data.accessToken)
            }
        }

        if (!response.status.isSuccess()) {
            error(response.bodyAsText())
        }
    }

    private suspend fun retryRateLimited(request: suspend () -> HttpResponse): HttpResponse {
        repeat(MAX_RATE_LIMIT_RETRIES + 1) { attempt ->
            val response = request()

            if (response.status != HttpStatusCode.TooManyRequests || attempt == MAX_RATE_LIMIT_RETRIES) {
                return response
            }

            val retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.toDoubleOrNull()
                ?: runCatching { response.body<OAuth2RateLimitResponse>().retryAfter }.getOrDefault(1.0)

            delay(retryAfterSeconds.coerceAtLeast(0.0).seconds)
        }

        error("Rate-limit retry loop completed unexpectedly")
    }

    override fun close() = httpClient.close()

    private companion object {
        const val MAX_RATE_LIMIT_RETRIES = 3
    }
}

data object OAuth2TokenExchangeError : IError

internal sealed interface OAuth2TokenRefreshError : IError {
    data object InvalidToken : OAuth2TokenRefreshError
    data object TemporaryFailure : OAuth2TokenRefreshError
}

@Serializable
private data class OAuth2ErrorResponse(val error: String)

@Serializable
private data class OAuth2RateLimitResponse(@SerialName("retry_after") val retryAfter: Double)
