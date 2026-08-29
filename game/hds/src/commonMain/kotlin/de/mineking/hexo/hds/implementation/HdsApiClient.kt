package de.mineking.hexo.hds.implementation

import de.mineking.hexo.game.model.RepositoryContainer
import de.mineking.hexo.game.model.RepositoryWrapper
import de.mineking.hexo.hds.implementation.formation.FormationRepositoryImpl
import de.mineking.hexo.hds.implementation.game.FinishedGameRepositoryImpl
import de.mineking.hexo.hds.implementation.leaderboard.LeaderboardRepositoryImpl
import de.mineking.hexo.hds.implementation.profile.ProfileRepositoryImpl
import de.mineking.hexo.hds.implementation.session.SessionRepositoryImpl
import de.mineking.hexo.hds.implementation.socket.SocketIOClient
import de.mineking.hexo.hds.implementation.tournament.TournamentRepositoryImpl
import de.mineking.hexo.hds.implementation.utils.EntityRequesterFactory
import de.mineking.hexo.hds.implementation.utils.logRequestErrors
import de.mineking.hexo.utils.coroutines.createCoroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json

const val DEFAULT_HOST = "https://hexo.did.science"

private val logger = KotlinLogging.logger {}

internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

class HdsApiClient(
    internal val coroutineScope: CoroutineScope = createCoroutineScope(logger),
    internal val host: String = DEFAULT_HOST,
    internal val socketClient: SocketIOClient?,
    private val httpClient: HttpClient = createDefaultHttpClient(),
    internal val entityRequesterFactory: EntityRequesterFactory = EntityRequesterFactory.Debouncing(coroutineScope).logRequestErrors(),
    repositoryWrapper: RepositoryWrapper = RepositoryWrapper,
) : RepositoryContainer {
    internal suspend fun request(path: String, builder: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
        httpClient.request("$host/api$path", builder)

    override val formationRepository = repositoryWrapper.run { FormationRepositoryImpl(this@HdsApiClient).wrap() }
    override val finishedGameRepository = repositoryWrapper.run { FinishedGameRepositoryImpl(this@HdsApiClient).wrap() }
    override val leaderboardRepository = repositoryWrapper.run { LeaderboardRepositoryImpl(this@HdsApiClient).wrap() }
    override val profileRepository = repositoryWrapper.run { ProfileRepositoryImpl(this@HdsApiClient).wrap() }
    override val sessionRepository = repositoryWrapper.run { SessionRepositoryImpl(this@HdsApiClient).wrap() }
    override val tournamentRepository = repositoryWrapper.run { TournamentRepositoryImpl(this@HdsApiClient).wrap() }

    fun shutdown() {
        coroutineScope.cancel()
        socketClient?.disconnect()
    }
}

expect val DefaultHttpEngine: HttpClientEngine

expect val HEXO_USER_AGENT: String?

fun createDefaultHttpClient(
    engine: HttpClientEngine = DefaultHttpEngine,
    config: HttpClientConfig<*>.() -> Unit = {},
) = HttpClient(engine) {
    install(ContentNegotiation) {
        json(json)
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.UserAgent, HEXO_USER_AGENT)
    }

    config()
}
