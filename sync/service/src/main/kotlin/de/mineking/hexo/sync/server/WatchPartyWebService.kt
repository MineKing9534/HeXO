package de.mineking.hexo.sync.server

import de.mineking.hexo.sever.service.ApiWebService
import de.mineking.hexo.sync.common.WatchPartyErrorResponse
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.sync.common.WatchPartyPingRequest
import de.mineking.hexo.sync.common.WatchPartyPongResponse
import de.mineking.hexo.sync.common.WatchPartyRequest
import de.mineking.hexo.sync.common.WatchPartyResponse
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
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}
private val connectionCleanupTimeout = 5.seconds
private val sessionRemovalTimeout = 5.minutes

class WatchPartyWebService : ApiWebService() {
    private val sessions = ConcurrentHashMap<WatchPartyId, WatchPartySession>()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun acquireSession(id: WatchPartyId, connectionId: WatchPartyConnectionId): WatchPartySession? {
        val session = sessions[id] ?: return null
        return if (session.acquire(connectionId)) session else null
    }

    private fun scheduleRemoval(session: WatchPartySession) {
        val job = cleanupScope.launch {
            delay(sessionRemovalTimeout)

            if (session.markRemovingIfUnused()) {
                sessions.remove(session.id, session)
                logger.info { "Removed watchparty with id ${session.id.value}" }
            }
        }

        session.scheduleRemoval(job)
    }

    private fun createSession(connectionId: WatchPartyConnectionId): WatchPartySession {
        val id = WatchPartyId(Uuid.random().toString())
        logger.info { "Created watchparty with id ${id.value}" }

        return WatchPartySession(
            id = id,
            cleanupScope = cleanupScope,
            connectionCleanupTimeout = connectionCleanupTimeout,
        ).also {
            check(it.acquire(connectionId))
            sessions[id] = it
        }
    }

    override fun Application.setup() {
        install(WebSockets) {
            pingPeriod = 30.seconds
            timeout = 15.seconds
            contentConverter = KotlinxWebsocketSerializationConverter(Json {
                allowStructuredMapKeys = true
            })
        }
    }

    override fun Route.registerApiRoutes() {
        route("/watchparties") {
            webSocket("/ws") {
                val id = call.request.queryParameters["id"]?.let { WatchPartyId(it) }
                val detachOnClose = call.request.queryParameters["detachOnClose"] == "true"
                val connectionId = call.request.queryParameters["connectionId"]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { WatchPartyConnectionId(it) }
                    ?: WatchPartyConnectionId(Uuid.random().toString())

                val session = when {
                    id != null -> acquireSession(id, connectionId) ?: run {
                        close(CloseReason(WatchPartyWebsocketCodes.NotFound, "No session found with id ${id.value}"))
                        return@webSocket
                    }
                    else -> createSession(connectionId)
                }

                try {
                    handleConnection(session, connectionId = connectionId)
                } finally {
                    if (session.release(connectionId)) {
                        scheduleRemoval(session)
                    }

                    if (detachOnClose) {
                        session.update { it.copy(target = null) }
                    }
                }
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleConnection(
        session: WatchPartySession,
        connectionId: WatchPartyConnectionId,
    ) {
        val job = launch {
            session.collect(connectionId) {
                sendSerialized<WatchPartyResponse>(it)
            }
        }

        try {
            for (frame in incoming) {
                try {
                    val request = converter!!.deserialize<WatchPartyRequest>(frame)
                    if (request is WatchPartyPingRequest) {
                        sendSerialized<WatchPartyResponse>(WatchPartyPongResponse)
                    } else {
                        session.apply(request, connectionId)
                    }
                } catch (e: SerializationException) {
                    sendSerialized<WatchPartyResponse>(WatchPartyErrorResponse(e.message ?: "Invalid request"))
                } catch (e: WatchPartyRequestException) {
                    sendSerialized<WatchPartyResponse>(WatchPartyErrorResponse(e.message))
                }
            }
        } finally {
            job.cancelAndJoin()
        }
    }
}
