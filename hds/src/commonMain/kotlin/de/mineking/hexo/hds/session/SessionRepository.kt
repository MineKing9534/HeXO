package de.mineking.hexo.hds.session

import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.socket.GameCellPlace
import de.mineking.hexo.hds.socket.GameStateUpdated
import de.mineking.hexo.hds.socket.HexoSocketRequest
import de.mineking.hexo.hds.socket.LobbyRemoved
import de.mineking.hexo.hds.socket.LobbyUpdated
import de.mineking.hexo.hds.socket.SessionUpdated
import de.mineking.hexo.hds.socket.SessionWatchError
import de.mineking.hexo.hds.socket.SessionWatchStarted
import de.mineking.hexo.hds.socket.SocketIOClient
import de.mineking.hexo.hds.socket.SocketListener
import de.mineking.hexo.hds.socket.listen
import de.mineking.hexo.hds.utils.EntityState
import de.mineking.hexo.hds.utils.withLock
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

private val logger = KotlinLogging.logger {}

interface SessionRepository {
    val lobbies: StateFlow<Map<SessionId, LobbySession>>

    fun observeSession(id: SessionId): StateFlow<EntityState<Session>>
}

internal class SessionRepositoryImpl(private val client: HdsApiClient) : SessionRepository {
    private val lobbyInitialization = CompletableDeferred<Unit>()
    override val lobbies = MutableStateFlow(emptyMap<SessionId, LobbySession>())

    private val sessionsLock = SynchronizedObject()
    private val sessions = mutableMapOf<SessionId, MutableStateFlow<EntityState<Session>>>()

    init {
        client.socketClient?.registerLobbyListeners()
        client.coroutineScope.launch { populateLobbyList() }
    }

    private suspend fun populateLobbyList() {
        val response = client.request("/sessions")
        val lobbies = response.body<List<LobbyInfoDto>>()

        this.lobbies.value = lobbies.associate { it.id to LobbySession.of(this, it) }
        lobbyInitialization.complete(Unit)
    }

    private fun SocketIOClient.registerLobbyListeners() {
        listen<LobbyUpdated> { event ->
            val oldLobby = lobbies.value[event.id]
            val newLobby = LobbySession.of(this@SessionRepositoryImpl, event.data)
            lobbies.update { it + (event.id to newLobby) }

            if (oldLobby == null || !(!oldLobby.hasStarted() && newLobby.hasStarted())) return@listen
            sessionsLock.withLock {
                val state = sessions[event.id]?.value ?: return@listen
                if (state is EntityState.Data && state.value is LobbySession) {
                    sessions[event.id]?.populate(event.id)
                }
            }
        }
        listen<LobbyRemoved> { event ->
            lobbies.update { it - event.id }
            sessionsLock.withLock {
                sessions -= event.id
            }
        }
    }

    private fun LiveSession.createLastState() = dto.state
        .let { it as? SessionStateDto.InGame }
        ?.let { Clock.System.now() to it }
        ?: lastState

    override fun observeSession(id: SessionId): StateFlow<EntityState<Session>> {
        if (client.socketClient == null) error("Cannot observe sessions without a SocketIO connection")

        return sessionsLock.withLock {
            sessions.getOrPut(id) {
                MutableStateFlow<EntityState<Session>>(EntityState.Loading).apply {
                    lobbyInitialization.invokeOnCompletion {
                        val lobby = lobbies.value[id]
                        if (lobby != null && !lobby.hasStarted()) {
                            value = EntityState.Data(lobby)
                        } else {
                            populate(id)
                        }
                    }
                }
            }
        }
    }

    private fun MutableStateFlow<EntityState<Session>>.handleSessionState(id: SessionId, onCleanup: () -> Unit) {
        require(client.socketClient != null)

        val listeners = mutableListOf<SocketListener>()
        fun cleanup() {
            client.socketClient.request(HexoSocketRequest.UnwatchSession(id))

            sessionsLock.withLock { sessions -= id }

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

                val value = state.value as? LiveSession ?: return@listen

                val session = LiveSession.of(
                    client = this@SessionRepositoryImpl.client,
                    dto = value.dto.copy(
                        state = event.session.state ?: value.dto.state,
                        players = event.session.players ?: value.dto.players,
                    ),
                    lastState = value.createLastState(),
                    gameState = value.gameState,
                )

                if (
                    event.session.state is SessionStateDto.Finished &&
                    session.players.any { it.connectionStatus == SessionPlayerConnectionStatus.Disconnected }
                ) {
                    logger.info { "Session ${id.value} removed because it has finished" }
                    cleanup()
                }

                EntityState.Data(session)
            }
        }
    }

    private fun MutableStateFlow<EntityState<Session>>.populate(id: SessionId) {
        require(client.socketClient != null)

        val listeners = mutableListOf<SocketListener>()

        listeners += client.socketClient.listen<SessionWatchStarted> { event ->
            if (event.session.id != id) return@listen

            logger.info { "Successfully joined session ${event.session.id.value}" }
            this@populate.value = EntityState.Data(LiveSession.of(
                client = this@SessionRepositoryImpl.client,
                dto = event.session,
                lastState = null,
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

                val value = state.value as? LiveSession ?: return@listen

                EntityState.Data(LiveSession.of(
                    client = this@SessionRepositoryImpl.client,
                    dto = value.dto,
                    lastState = value.createLastState(),
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

                val value = state.value as? LiveSession ?: return@listen
                EntityState.Data(LiveSession.of(
                    client = this@SessionRepositoryImpl.client,
                    dto = value.dto,
                    lastState = value.createLastState(),
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
