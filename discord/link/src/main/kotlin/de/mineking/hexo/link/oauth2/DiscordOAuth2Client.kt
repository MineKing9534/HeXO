package de.mineking.hexo.link.oauth2

import de.mineking.hexo.discord.core.DiscordUserId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DiscordOAuth2Client(
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String,
) {
    companion object {
        private const val BASE_URL = "https://discord.com/api"
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    fun generateAuthorizationUrl(vararg scopes: Scope, state: String) = URLBuilder("https://discord.com/oauth2/authorize").apply {
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

        val response = client.get("$BASE_URL/oauth2/@me") {
            bearerAuth(accessToken)
        }.body<Response>()

        return response.user.id
    }

    internal suspend fun getUserTokens(code: String): OAuth2Tokens? {
        val response = client.submitForm(
            "$BASE_URL/oauth2/token",
            formParameters = parameters {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
            },
        ) {
            basicAuth(clientId, clientSecret)
        }

        if (!response.status.isSuccess()) return null

        val data = response.body<OAuth2TokensDto>()
        return OAuth2Tokens(this, data, getCurrentUserId(data.accessToken))
    }

    internal suspend fun refreshToken(refreshToken: String): OAuth2Tokens? {
        val response = client.submitForm(
            "$BASE_URL/oauth2/token",
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            },
        ) {
            basicAuth(clientId, clientSecret)
        }

        if (!response.status.isSuccess()) return null

        val data = response.body<OAuth2TokensDto>()
        return OAuth2Tokens(this, data, getCurrentUserId(data.accessToken))
    }

    internal suspend fun revokeToken(tokens: OAuth2Tokens) {
        client.submitForm(
            "$BASE_URL/oauth2/token/revoke",
            formParameters = parameters {
                append("token", tokens.data.accessToken)
            },
        ) {
            basicAuth(clientId, clientSecret)
        }
    }

    suspend fun updateLinkedRoleData(user: OAuth2Tokens, vararg values: LinkedRoleMetadataValue<*>) {
        @Serializable
        data class Request(val metadata: Map<String, String>)

        val response = client.put("$BASE_URL/users/@me/applications/$clientId/role-connection") {
            val request = Request(values.filter { it.value != null }.associate { it.key.key to it.value.toString() })
            setBody(request)
            bearerAuth(user.data.accessToken)
        }

        if (!response.status.isSuccess()) {
            error(response.bodyAsText())
        }
    }
}
