package de.mineking.hexo.hds.implementation.session

import de.mineking.hexo.game.model.session.DetailedSession
import de.mineking.hexo.game.model.session.Session
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.game.model.session.SessionNotFoundError
import de.mineking.hexo.game.model.session.SessionPlayerConnectionStatus
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.session.SessionSelector
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.parseBodyOrNull
import de.mineking.hexo.hds.implementation.socket.GameCellPlace
import de.mineking.hexo.hds.implementation.socket.GameStateUpdated
import de.mineking.hexo.hds.implementation.socket.HdsSocketClient
import de.mineking.hexo.hds.implementation.socket.HdsSocketRequest
import de.mineking.hexo.hds.implementation.socket.LobbyRemoved
import de.mineking.hexo.hds.implementation.socket.LobbyUpdated
import de.mineking.hexo.hds.implementation.socket.SessionUpdated
import de.mineking.hexo.hds.implementation.socket.SessionWatchError
import de.mineking.hexo.hds.implementation.socket.SessionWatchStarted
import de.mineking.hexo.hds.implementation.utils.withLock
import de.mineking.hexo.utils.types.EntityState
import de.mineking.hexo.utils.types.QueryResult
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

internal class SessionRepositoryImpl(private val client: HdsApiClient) : SessionRepository {
    override val url = "${client.publicUrl}/session"

    override val sessions = MutableStateFlow(emptyMap<SessionId, SessionImpl>())

    private val sessionsLock = SynchronizedObject()
    private val sessionFlows = mutableMapOf<SessionId, MutableStateFlow<EntityState<ObservedSessionImpl>>>()

    private val requester = client.entityRequesterFactory.createEntityRequester<SessionId, Session?> { id ->
        val response = client.request("/session/${id.value}")

        response.parseBodyOrNull<SessionDto, Session> {
            DetailedSessionImpl(client, it)
        }
    }

    private val listRequester = client.entityRequesterFactory.createEntityRequester<Unit, List<SessionImpl>> {
        val response = client.request("/sessions")
        val lobbies = response.body<List<LobbyInfoDto>>()

        lobbies.map { SessionImpl(client, it) }
    }

    init {
        client.client.socketClient?.run {
            registerLobbyListeners()
            registerSessionListeners()
        }
        client.client.httpClient.launch { populateLobbyList() }
    }

    override suspend fun getSessions(selector: SessionSelector): QueryResult<Session> {
        val sessions = listRequester.fetch(Unit)
            .filter {
                val filter = selector.filter?.rated ?: return@filter true
                it.gameOptions.rated == filter
            }

        val result = sessions
            .drop(selector.offset ?: 0)
            .take(selector.limit ?: Int.MAX_VALUE)

        return object : QueryResult<Session>, Flow<Session> by result.asFlow() {
            override val totalCount = sessions.size
        }
    }

    private suspend fun populateLobbyList() {
        this.sessions.value = listRequester.fetch(Unit)
            .associateBy { it.id }
    }

    private fun HdsSocketClient.registerLobbyListeners() {
        listen<LobbyUpdated> { (event) ->
            val newLobby = SessionImpl(client, event.data)
            sessions.update { it + (event.id to newLobby) }
        }
        listen<LobbyRemoved> { (event) ->
            sessions.update { it - event.id }
        }
    }

    override suspend fun getSession(id: SessionId) = requester.fetch(id)
        .successIfNotNullOrElse(SessionNotFoundError)

    override fun observeSession(id: SessionId): StateFlow<EntityState<DetailedSession>> {
        val socketClient = client.client.socketClient ?: error("Cannot observe sessions without a SocketIO connection")
        var created = false

        val flow = sessionsLock.withLock {
            sessionFlows[id] ?: MutableStateFlow<EntityState<ObservedSessionImpl>>(EntityState.Loading).also {
                sessionFlows[id] = it
                created = true
            }
        }
        if (created) {
            logger.info { "Watching session ${id.value}..." }
            socketClient.request(HdsSocketRequest.WatchSession(id))
        }
        return flow
    }

    private fun sessionFlow(id: SessionId) = sessionsLock.withLock { sessionFlows[id] }

    private fun HdsSocketClient.cleanupSession(id: SessionId) {
        request(HdsSocketRequest.UnwatchSession(id))
        sessionsLock.withLock { sessionFlows -= id }
    }

    private fun HdsSocketClient.registerSessionListeners() {
        registerSessionWatchStartedListener()
        registerSessionWatchErrorListener()
        registerSessionUpdatedListener()
        registerGameCellPlaceListener()
        registerGameStateUpdatedListener()
    }

    private fun HdsSocketClient.registerSessionWatchStartedListener() {
        listen<SessionWatchStarted> { (event) ->
            val flow = sessionFlow(event.session.id) ?: return@listen

            logger.info { "Successfully joined session ${event.session.id.value}" }
            flow.value = EntityState.Data(ObservedSessionImpl.of(
                client = client,
                dto = event.session,
                gameState = event.gameState,
            ))
        }
    }

    private fun HdsSocketClient.registerSessionWatchErrorListener() {
        listen<SessionWatchError> { (event) ->
            val flow = sessionFlow(event.sessionId) ?: return@listen

            logger.warn { "Failed to watch session ${event.sessionId.value}: ${event.message}" }
            flow.value = EntityState.NotFound
            cleanupSession(event.sessionId)
        }
    }

    private fun HdsSocketClient.registerSessionUpdatedListener() {
        listen<SessionUpdated> { (event) ->
            val flow = sessionFlow(event.sessionId) ?: return@listen
            var cleanup = false

            flow.update { state ->
                if (state !is EntityState.Data) {
                    logger.warn { "Received session-updated event for unconnected session ${event.sessionId.value}" }
                    return@update state
                }

                val session = ObservedSessionImpl.of(
                    client = client,
                    dto = state.value.dto.copy(
                        state = event.session.state ?: state.value.dto.state,
                        players = event.session.players ?: state.value.dto.players,
                    ),
                    gameState = state.value.gameState,
                )

                if (
                    event.session.state is SessionStateDto.Finished &&
                    session.players.all { it.connectionStatus == SessionPlayerConnectionStatus.Disconnected }
                ) {
                    cleanup = true
                }

                EntityState.Data(session)
            }
            if (cleanup) {
                logger.info { "Session ${event.sessionId.value} removed because it has finished" }
                cleanupSession(event.sessionId)
            }
        }
    }

    private fun HdsSocketClient.registerGameCellPlaceListener() {
        listen<GameCellPlace> { (event) ->
            val flow = sessionFlow(event.sessionId) ?: return@listen

            flow.update { state ->
                if (state !is EntityState.Data || event.sessionId != state.value.id) {
                    logger.warn { "Received game-cell-place event for unconnected session ${event.sessionId.value}" }
                    return@update state
                }

                val value = state.value as? LiveSessionImpl ?: return@update state

                EntityState.Data(ObservedSessionImpl.of(
                    client = client,
                    dto = value.dto,
                    gameState = event.state.copy(
                        cells = (value.gameState.cells ?: emptyList()) + event.cell,
                        playerTiles = value.gameState.playerTiles,
                    ),
                ))
            }
        }
    }

    private fun HdsSocketClient.registerGameStateUpdatedListener() {
        listen<GameStateUpdated> { (event) ->
            val flow = sessionFlow(event.sessionId) ?: return@listen

            flow.update { state ->
                if (state !is EntityState.Data || event.sessionId != state.value.id) {
                    logger.warn { "Received game-state event for unconnected session ${event.sessionId.value}" }
                    return@update state
                }

                EntityState.Data(ObservedSessionImpl.of(
                    client = client,
                    dto = state.value.dto,
                    gameState = event.gameState,
                ))
            }
        }
    }
}
