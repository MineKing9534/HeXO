package de.mineking.hexo.sync.server

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.plusAssign
import de.mineking.hexo.core.omitted
import de.mineking.hexo.sever.service.ApiWebService
import de.mineking.hexo.sync.common.WatchPartyCellRequest
import de.mineking.hexo.sync.common.WatchPartyData
import de.mineking.hexo.sync.common.WatchPartyErrorResponse
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.sync.common.WatchPartyLineHighlightRequest
import de.mineking.hexo.sync.common.WatchPartyMoveCountRequest
import de.mineking.hexo.sync.common.WatchPartyNavigateRequest
import de.mineking.hexo.sync.common.WatchPartyNavigateTarget
import de.mineking.hexo.sync.common.WatchPartyRequest
import de.mineking.hexo.sync.common.WatchPartyResponse
import de.mineking.hexo.sync.common.WatchPartyTarget
import de.mineking.hexo.sync.common.WatchPartyUpdateRequest
import de.mineking.hexo.sync.common.WatchPartyWebsocketCodes
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.deserialize
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.converter
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}
private val sessionRemovalTimeout = 5.minutes

class WatchPartyWebService : ApiWebService() {
    data class Session(
        val data: MutableStateFlow<WatchPartyData>,
        val lock: Mutex = Mutex(),
        val connections: AtomicInteger = AtomicInteger(1),
        val removalJob: AtomicReference<Job?> = AtomicReference(null),
    )
    private val sessions = ConcurrentHashMap<WatchPartyId, Session>()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun acquireSession(id: WatchPartyId): Session? {
        val session = sessions[id] ?: return null

        while (true) {
            val connections = session.connections.get()
            if (connections < 0) return null
            if (session.connections.compareAndSet(connections, connections + 1)) {
                session.removalJob.getAndSet(null)?.cancel()
                return session
            }
        }
    }

    private fun scheduleRemoval(session: Session) {
        val job = cleanupScope.launch {
            delay(sessionRemovalTimeout)

            if (session.connections.compareAndSet(0, -1)) {
                sessions.remove(session.data.value.id, session)
                logger.info { "Removed watchparty with id ${session.data.value.id.value}" }
            }
        }

        session.removalJob.getAndSet(job)?.cancel()
    }

    private fun createSession(): Session {
        val data = WatchPartyData(
            id = WatchPartyId(Uuid.random().toString()),
            target = null,
        )

        logger.info { "Created watchparty with id ${data.id.value}" }

        return Session(MutableStateFlow(data))
            .also { sessions[data.id] = it }
    }

    override fun Application.setup() {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json {
                allowStructuredMapKeys = true
            })
        }
    }

    override fun Route.registerApiRoutes() {
        route("/watchparties") {
            webSocket("/ws") {
                val id = call.request.queryParameters["id"]?.let { WatchPartyId(it) }
                val session = when {
                    id != null -> acquireSession(id) ?: run {
                        close(CloseReason(WatchPartyWebsocketCodes.NotFound, "No session found with id ${id.value}"))
                        return@webSocket
                    }
                    else -> createSession()
                }

                handleConnection(session)
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleConnection(session: Session) {
        val job = launch {
            session.data.collect {
                sendSerialized<WatchPartyResponse>(it)
            }
        }

        try {
            for (frame in incoming) {
                try {
                    val request = converter!!.deserialize<WatchPartyRequest>(frame)
                    session.applyRequest(request)
                } catch (e: SerializationException) {
                    sendSerialized<WatchPartyResponse>(WatchPartyErrorResponse(e.message ?: "Invalid request"))
                }
            }
        } finally {
            job.cancelAndJoin()
            if (session.connections.decrementAndGet() == 0) {
                scheduleRemoval(session)
            }
        }
    }

    context(wsSession: DefaultWebSocketServerSession)
    private suspend fun Session.applyRequest(request: WatchPartyRequest): Unit = lock.withLock {
        data.update {
            when (request) {
                is WatchPartyNavigateRequest -> it.applyNavigateRequest(request)
                is WatchPartyUpdateRequest -> it.applyUpdateRequest(request) ?: return
                is WatchPartyMoveCountRequest -> it.applyMoveCountRequest(request) ?: return
                is WatchPartyCellRequest -> it.applyCellRequest(request) ?: return
                is WatchPartyLineHighlightRequest -> it.applyLineHighlightRequest(request) ?: return
            }
        }
    }

    private fun WatchPartyData.applyNavigateRequest(request: WatchPartyNavigateRequest): WatchPartyData {
        return copy(target = when (val target = request.target) {
            is WatchPartyNavigateTarget.Sandbox -> WatchPartyTarget.Sandbox(Board())
            is WatchPartyNavigateTarget.Session -> WatchPartyTarget.Session(
                sessionId = target.id,
                move = Int.MAX_VALUE,
                overlay = Board(),
            )
            null -> null
        })
    }

    context(wsSession: DefaultWebSocketServerSession)
    private suspend fun WatchPartyData.applyUpdateRequest(request: WatchPartyUpdateRequest): WatchPartyData? {
        val target = target
            ?: return null.also { wsSession.sendSerialized(WatchPartyErrorResponse("no watchparty target")) }

        return copy(target = when (target) {
            is WatchPartyTarget.Session -> target.copy(overlay = request.board.removeOwners())
            is WatchPartyTarget.Sandbox -> target.copy(board = request.board)
        })
    }

    context(wsSession: DefaultWebSocketServerSession)
    private suspend fun WatchPartyData.applyMoveCountRequest(request: WatchPartyMoveCountRequest): WatchPartyData? {
        val target = target as? WatchPartyTarget.Session
            ?: return null.also { wsSession.sendSerialized(WatchPartyErrorResponse("invalid watchparty target")) }

        return copy(target = target.copy(move = request.move))
    }

    context(wsSession: DefaultWebSocketServerSession)
    private suspend fun WatchPartyData.applyCellRequest(request: WatchPartyCellRequest): WatchPartyData? {
        val target = target
            ?: return null.also { wsSession.sendSerialized(WatchPartyErrorResponse("no watchparty target")) }

        return copy(target = when (target) {
            is WatchPartyTarget.Session -> target.copy(overlay = target.overlay.copy().apply {
                this@apply[request.coordinate] += request.cell.copy(owner = omitted())
            })
            is WatchPartyTarget.Sandbox -> target.copy(board = target.board.copy().apply {
                this@apply[request.coordinate] += request.cell.copy()
            })
        })
    }

    context(wsSession: DefaultWebSocketServerSession)
    private suspend fun WatchPartyData.applyLineHighlightRequest(request: WatchPartyLineHighlightRequest): WatchPartyData? {
        val target = target
            ?: return null.also { wsSession.sendSerialized(WatchPartyErrorResponse("no watchparty target")) }

        fun Board.applyRequest() = copy().apply {
            if (request.remove) {
                lineHighlights -= request.line
            } else {
                lineHighlights += request.line
            }
        }

        return copy(target = when (target) {
            is WatchPartyTarget.Session -> target.copy(overlay = target.overlay.applyRequest())
            is WatchPartyTarget.Sandbox -> target.copy(board = target.board.applyRequest())
        })
    }
}

private fun Board.removeOwners() = copy().apply {
    cells.forEach { (_, cell) ->
        cell.owner = null
    }
}
