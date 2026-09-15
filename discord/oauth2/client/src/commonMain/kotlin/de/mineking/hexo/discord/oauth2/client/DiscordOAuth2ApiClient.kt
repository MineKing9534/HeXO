package de.mineking.hexo.discord.oauth2.client

import de.mineking.hexo.discord.oauth2.model.OAuth2AuthorizationResponse
import de.mineking.hexo.discord.oauth2.model.OAuth2CallbackResponse
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class DiscordOAuth2ApiClient(
    private val apiUrl: String,
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json)
        }
    },
) : AutoCloseable {
    suspend fun createAuthorization(flow: OAuth2Flow): String {
        val response = client.get("${apiUrl.trimEnd('/')}/oauth2/authorization") {
            parameter("flow", flow.value)
        }
        check(response.status.isSuccess()) { "Could not create Discord authorization: ${response.status}" }
        return response.body<OAuth2AuthorizationResponse>().url
    }

    suspend fun completeAuthorization(code: String, state: String): OAuth2CallbackResponse {
        val response = client.get("${apiUrl.trimEnd('/')}/oauth2/callback") {
            parameter("code", code)
            parameter("state", state)
        }
        return response.body()
    }

    override fun close() = client.close()
}
