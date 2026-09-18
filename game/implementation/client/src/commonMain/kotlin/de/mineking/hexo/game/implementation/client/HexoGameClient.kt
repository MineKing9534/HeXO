package de.mineking.hexo.game.implementation.client

import de.mineking.hexo.game.implementation.client.profile.ProfileRepositoryImpl
import de.mineking.hexo.game.model.RepositoryContainer
import de.mineking.hexo.game.model.RepositoryWrapper
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.tournament.TournamentRepository
import de.mineking.hexo.utils.types.EntityRequesterFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val DEFAULT_HMD_API_URL = "https://hexo.mineking.dev/api"
const val DEFAULT_HMD_PUBLIC_URL = "https://hexo.mineking.dev"

internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    allowStructuredMapKeys = true
}

data class HexoHttpClient(
    val apiUrl: String,
    val httpClient: HttpClient,
) : AutoCloseable {
    companion object {
        fun createDefault(
            apiUrl: String = DEFAULT_HMD_API_URL,
            httpClient: HttpClient = HttpClient {
                install(ContentNegotiation) {
                    json(json)
                }

                defaultRequest {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.UserAgent, HEXO_USER_AGENT)
                }
            },
        ) = HexoHttpClient(
            apiUrl = apiUrl,
            httpClient = httpClient,
        )
    }

    override fun close() {
        httpClient.close()
    }
}

class HexoGameClient(
    internal val client: HexoHttpClient,
    internal val publicUrl: String = DEFAULT_HMD_PUBLIC_URL,
    internal val entityRequesterFactory: EntityRequesterFactory = EntityRequesterFactory.Debouncing(client.httpClient),
    repositoryWrapper: RepositoryWrapper = RepositoryWrapper,
) : RepositoryContainer, AutoCloseable {
    internal suspend fun request(path: String, builder: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
        client.httpClient.request("${client.apiUrl.trimEnd('/')}$path", builder)

    override val formationRepository = repositoryWrapper.run { FormationRepository.wrap() }
    override val finishedGameRepository = repositoryWrapper.run { FinishedGameRepository.wrap() }
    override val leaderboardRepository = repositoryWrapper.run { LeaderboardRepository.wrap() }
    override val profileRepository = repositoryWrapper.run { ProfileRepositoryImpl(this@HexoGameClient).wrap() }
    override val sessionRepository = repositoryWrapper.run { SessionRepository.wrap() }
    override val tournamentRepository = repositoryWrapper.run { TournamentRepository.wrap() }

    override fun close() {
        client.close()
    }
}

internal expect val HEXO_USER_AGENT: String?
