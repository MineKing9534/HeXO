package de.mineking.hexo.sync.server

import de.mineking.hexo.sever.service.ApiWebService
import de.mineking.hexo.sync.common.WatchPartyCellHighlightRequest
import de.mineking.hexo.sync.common.WatchPartyData
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.sync.common.WatchPartyLineHighlightRequest
import de.mineking.hexo.sync.common.WatchPartyNavigateRequest
import de.mineking.hexo.sync.common.WatchPartyRequest
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
import io.ktor.websocket.send
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
            sessionId = null,
            cellHighlights = emptyMap(),
            lineHighlights = emptyList(),
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
        route("/sessions/sync") {
            webSocket("/gateway") {
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
                sendSerialized(it)
            }
        }

        try {
            for (frame in incoming) {
                try {
                    val request = converter!!.deserialize<WatchPartyRequest>(frame)
                    session.applyRequest(request)
                } catch (e: SerializationException) {
                    send(e.message ?: "Invalid request")
                }
            }
        } finally {
            job.cancelAndJoin()
            if (session.connections.decrementAndGet() == 0) {
                scheduleRemoval(session)
            }
        }
    }

    private suspend fun Session.applyRequest(request: WatchPartyRequest) = lock.withLock {
        when (request) {
            is WatchPartyUpdateRequest -> data.update { it.copy(cellHighlights = request.cellHighlights, lineHighlights = request.lineHighlights) }
            is WatchPartyNavigateRequest -> data.update {
                if (request.sessionId == it.sessionId) return@update it
                it.copy(
                    sessionId = request.sessionId,
                    lineHighlights = emptyList(),
                    cellHighlights = emptyMap(),
                )
            }
            is WatchPartyCellHighlightRequest -> data.update {
                val highlights = it.cellHighlights.toMutableMap()
                val highlight = request.highlight

                if (highlight == null) {
                    highlights -= request.coordinate
                } else {
                    highlights[request.coordinate] = highlight
                }

                it.copy(cellHighlights = highlights)
            }
            is WatchPartyLineHighlightRequest -> data.update {
                val highlights = it.lineHighlights.toMutableList()

                if (request.remove) {
                    highlights -= request.line
                } else {
                    highlights += request.line
                }

                it.copy(lineHighlights = highlights)
            }
        }
    }
}
