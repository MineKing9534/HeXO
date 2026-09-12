package de.mineking.hexo.hds.implementation.session

import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.game.model.session.DetailedSession
import de.mineking.hexo.game.model.session.LiveSessionPlayer
import de.mineking.hexo.game.model.session.Session
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.game.model.session.SessionNotFoundError
import de.mineking.hexo.game.model.session.SessionPlayerConnectionStatus
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.socket.GameCellPlace
import de.mineking.hexo.hds.implementation.socket.GameStateUpdated
import de.mineking.hexo.hds.implementation.socket.HexoSocketRequest
import de.mineking.hexo.hds.implementation.socket.LobbyRemoved
import de.mineking.hexo.hds.implementation.socket.LobbyUpdated
import de.mineking.hexo.hds.implementation.socket.SessionUpdated
import de.mineking.hexo.hds.implementation.socket.SessionWatchError
import de.mineking.hexo.hds.implementation.socket.SessionWatchStarted
import de.mineking.hexo.hds.implementation.socket.SocketIOClient
import de.mineking.hexo.hds.implementation.socket.SocketListener
import de.mineking.hexo.hds.implementation.socket.listen
import de.mineking.hexo.hds.implementation.utils.parseBodyOrNull
import de.mineking.hexo.hds.implementation.utils.withLock
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

internal class SessionRepositoryImpl(private val client: HdsApiClient) : SessionRepository {
    override val url = "${client.host}/session"

    override val sessions = MutableStateFlow(emptyMap<SessionId, SessionImpl>())

    private val sessionsLock = SynchronizedObject()
    private val sessionFlows = mutableMapOf<SessionId, MutableStateFlow<EntityState<ObservedSessionImpl>>>()

    private val requester = client.entityRequesterFactory.createEntityRequester<SessionId, Session?> { id ->
        val response = client.request("/session/${id.value}")

        response.parseBodyOrNull<SessionDto, Session> {
            DetailedSessionImpl(client, it)
        }
    }

    init {
        client.socketClient?.registerLobbyListeners()
        client.coroutineScope.launch { populateLobbyList() }
    }

    private suspend fun populateLobbyList() {
        val response = client.request("/sessions")
        val lobbies = response.body<List<LobbyInfoDto>>()

        this.sessions.value = lobbies.associate { it.id to SessionImpl(client, it) }
    }

    private fun SocketIOClient.registerLobbyListeners() {
        listen<LobbyUpdated> { event ->
            val newLobby = SessionImpl(client, event.data)
            sessions.update { it + (event.id to newLobby) }
        }
        listen<LobbyRemoved> { event ->
            sessions.update { it - event.id }
            sessionsLock.withLock {
                sessionFlows -= event.id
            }
        }
    }

    override suspend fun getSession(id: SessionId) = requester.fetch(id)
        .successIfNotNullOrElse(SessionNotFoundError)

    override fun observeSession(id: SessionId): StateFlow<EntityState<DetailedSession>> {
        if (client.socketClient == null) error("Cannot observe sessions without a SocketIO connection")

        return sessionsLock.withLock {
            sessionFlows.getOrPut(id) {
                MutableStateFlow<EntityState<ObservedSessionImpl>>(EntityState.Loading).apply {
                    populate(id)
                }
            }
        }
    }

    private fun MutableStateFlow<EntityState<ObservedSessionImpl>>.handleSessionState(id: SessionId, onCleanup: () -> Unit) {
        require(client.socketClient != null)

        val listeners = mutableListOf<SocketListener>()
        fun cleanup() {
            client.socketClient.request(HexoSocketRequest.UnwatchSession(id))

            sessionsLock.withLock { sessionFlows -= id }

            listeners.forEach { it.remove() }
            onCleanup()
        }

        listeners += client.socketClient.listen<SessionWatchError> { event ->
            if (event.sessionId != id) return@listen

            logger.warn { "Failed to watch session ${id.value}: ${event.message}" }
            this@handleSessionState.value = EntityState.NotFound
            cleanup()
        }

        listeners += client.socketClient.listen<SessionUpdated> { event ->
            if (event.sessionId != id) return@listen

            update { state ->
                if (state !is EntityState.Data) {
                    logger.warn { "Received session-updated event for unconnected session ${event.sessionId.value}" }
                    return@listen
                }

                val session = ObservedSessionImpl.of(
                    client = this@SessionRepositoryImpl.client,
                    dto = state.value.dto.copy(
                        state = event.session.state ?: state.value.dto.state,
                        players = event.session.players ?: state.value.dto.players,
                    ),
                    gameState = state.value.gameState,
                )

                if (
                    event.session.state is SessionStateDto.Finished &&
                    session.players.any { it is LiveSessionPlayer && it.connectionStatus == SessionPlayerConnectionStatus.Disconnected }
                ) {
                    logger.info { "Session ${id.value} removed because it has finished" }
                    cleanup()
                }

                EntityState.Data(session)
            }
        }
    }

    private fun MutableStateFlow<EntityState<ObservedSessionImpl>>.populate(id: SessionId) {
        require(client.socketClient != null)

        val listeners = mutableListOf<SocketListener>()

        listeners += client.socketClient.listen<SessionWatchStarted> { event ->
            if (event.session.id != id) return@listen

            logger.info { "Successfully joined session ${event.session.id.value}" }
            this@populate.value = EntityState.Data(ObservedSessionImpl.of(
                client = this@SessionRepositoryImpl.client,
                dto = event.session,
                gameState = event.gameState,
            ))
        }

        listeners += client.socketClient.listen<GameCellPlace> { event ->
            if (event.sessionId != id) return@listen

            update { state ->
                if (state !is EntityState.Data || event.sessionId != state.value.id) {
                    logger.warn { "Received game-cell-place event for unconnected session ${event.sessionId.value}" }
                    return@listen
                }

                val value = state.value as? LiveSessionImpl ?: return@listen

                EntityState.Data(ObservedSessionImpl.of(
                    client = this@SessionRepositoryImpl.client,
                    dto = value.dto,
                    gameState = event.state.copy(
                        cells = (value.gameState.cells ?: emptyList()) + event.cell,
                        playerTiles = value.gameState.playerTiles,
                    ),
                ))
            }
        }

        listeners += client.socketClient.listen<GameStateUpdated> { event ->
            if (event.sessionId != id) return@listen

            update { state ->
                if (state !is EntityState.Data || event.sessionId != state.value.id) {
                    logger.warn { "Received game-state event for unconnected session ${event.sessionId.value}" }
                    return@listen
                }

                EntityState.Data(ObservedSessionImpl.of(
                    client = this@SessionRepositoryImpl.client,
                    dto = state.value.dto,
                    gameState = event.gameState,
                ))
            }
        }

        handleSessionState(id) {
            listeners.forEach { it.remove() }
        }

        logger.info { "Watching session ${id.value}..." }
        client.socketClient.request(HexoSocketRequest.WatchSession(id))
    }
}
