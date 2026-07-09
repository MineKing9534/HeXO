package de.mineking.hexo.sync.server

import de.mineking.hexo.sever.service.ApiWebService
import de.mineking.hexo.sync.common.SessionSyncCellHighlightRequest
import de.mineking.hexo.sync.common.SessionSyncData
import de.mineking.hexo.sync.common.SessionSyncId
import de.mineking.hexo.sync.common.SessionSyncLineHighlightRequest
import de.mineking.hexo.sync.common.SessionSyncNavigateRequest
import de.mineking.hexo.sync.common.SessionSyncRequest
import de.mineking.hexo.sync.common.SessionSyncUpdateRequest
import de.mineking.hexo.sync.common.SessionSyncWebsocketCodes
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}

class SessionSyncWebService : ApiWebService() {
    data class Session(
        val data: MutableStateFlow<SessionSyncData>,
        val lock: Mutex = Mutex(),
        val connections: AtomicInteger = AtomicInteger(1),
    )
    private val sessions = ConcurrentHashMap<SessionSyncId, Session>()

    private fun acquireSession(id: SessionSyncId): Session? {
        val session = sessions[id] ?: return null

        while (true) {
            val connections = session.connections.get()
            if (connections == 0) return null
            if (session.connections.compareAndSet(connections, connections + 1)) return session
        }
    }

    private fun createSession(): Session {
        val data = SessionSyncData(
            id = SessionSyncId(Uuid.random().toString()),
            sessionId = null,
            cellHighlights = emptyMap(),
            lineHighlights = emptyList(),
        )

        logger.info { "Created session sync with id ${data.id.value}" }

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
                val id = call.request.queryParameters["id"]?.let { SessionSyncId(it) }
                val session = when {
                    id != null -> acquireSession(id) ?: run {
                        close(CloseReason(SessionSyncWebsocketCodes.NotFound, "No session found with id ${id.value}"))
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
                    val request = converter!!.deserialize<SessionSyncRequest>(frame)
                    session.applyRequest(request)
                } catch (e: SerializationException) {
                    send(e.message ?: "Invalid request")
                }
            }
        } finally {
            job.cancelAndJoin()
            if (session.connections.decrementAndGet() == 0) {
                sessions.remove(session.data.value.id, session)
                logger.info { "Removed session sync with id ${session.data.value.id.value}" }
            }
        }
    }

    private suspend fun Session.applyRequest(request: SessionSyncRequest) = lock.withLock {
        when (request) {
            is SessionSyncUpdateRequest -> data.update { it.copy(cellHighlights = request.cellHighlights, lineHighlights = request.lineHighlights) }
            is SessionSyncNavigateRequest -> data.update {
                if (request.sessionId == it.sessionId) return@update it
                it.copy(
                    sessionId = request.sessionId,
                    lineHighlights = emptyList(),
                    cellHighlights = emptyMap(),
                )
            }
            is SessionSyncCellHighlightRequest -> data.update {
                val highlights = it.cellHighlights.toMutableMap()
                val highlight = request.highlight

                if (highlight == null) {
                    highlights -= request.coordinate
                } else {
                    highlights[request.coordinate] = highlight
                }

                it.copy(cellHighlights = highlights)
            }
            is SessionSyncLineHighlightRequest -> data.update {
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
