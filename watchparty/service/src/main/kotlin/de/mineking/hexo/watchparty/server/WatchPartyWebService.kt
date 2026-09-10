package de.mineking.hexo.watchparty.server

import de.mineking.hexo.sever.service.ApiWebService
import de.mineking.hexo.utils.socketio.server.SocketIO
import de.mineking.hexo.utils.socketio.server.SocketIOSession
import de.mineking.hexo.utils.socketio.server.socketIOSession
import de.mineking.hexo.watchparty.model.WatchPartyConnectionId
import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.protocol.WatchPartyAcknowledgement
import de.mineking.hexo.watchparty.protocol.WatchPartyClosedResponse
import de.mineking.hexo.watchparty.protocol.WatchPartyConnectData
import de.mineking.hexo.watchparty.protocol.WatchPartyCreatedResponse
import de.mineking.hexo.watchparty.protocol.WatchPartyNavigateRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.WebSockets
import io.socket.engineio.server.EngineIoServerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val logger = KotlinLogging.logger {}

class WatchPartyWebService(
    private val connectionCleanupTimeout: Duration = 5.seconds,
    private val sessionRemovalTimeout: Duration = 5.minutes,
) : ApiWebService() {
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

    private fun createSession(): WatchPartySession {
        val id = WatchPartyId(Uuid.random().toString())
        return WatchPartySession(id, cleanupScope, connectionCleanupTimeout).also {
            sessions[id] = it
            // Also expire parties that are created but never connected to.
            scheduleRemoval(it)
            logger.info { "Created watchparty with id ${id.value}" }
        }
    }

    override fun Application.setup() {
        install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.ContentType)
        }
        install(WebSockets)
        install(SocketIO) {
            format = Json { allowStructuredMapKeys = true }
            engineOptions.allowedCorsOrigins = EngineIoServerOptions.ALLOWED_CORS_ORIGIN_ALL
        }
        monitor.subscribe(ApplicationStopped) { cleanupScope.cancel() }
    }

    override fun Route.registerApiRoutes() {
        route("/watchparties") {
            post {
                call.respond(HttpStatusCode.Created, WatchPartyCreatedResponse(createSession().id))
            }

            socketIOSession<WatchPartyRequest, WatchPartyResponse> {
                handleConnection()
            }
        }
    }

    private suspend fun SocketIOSession<WatchPartyRequest, WatchPartyResponse>.handleConnection() {
        val data = try {
            connectionData<WatchPartyConnectData>().also {
                require(it.connectionId.value.isNotBlank())
            }
        } catch (_: IllegalArgumentException) {
            send(WatchPartyClosedResponse("Invalid connection data"))
            return
        }

        val session = acquireSession(data.watchPartyId, data.connectionId)
        if (session == null) {
            send(WatchPartyClosedResponse("Watchparty not found"))
            return
        }

        try {
            coroutineScope {
                val collector = launch {
                    session.state.collect(data.connectionId, id) {
                        send(it)
                    }
                }

                try {
                    handleRequests(session, data.connectionId)
                } finally {
                    collector.cancel()
                }
            }
        } finally {
            if (session.release(data.connectionId, data.detachOnClose)) {
                scheduleRemoval(session)
            }
        }
    }

    private suspend fun SocketIOSession<WatchPartyRequest, WatchPartyResponse>.handleRequests(
        session: WatchPartySession,
        connectionId: WatchPartyConnectionId,
    ) {
        for (event in incoming) {
            val result = try {
                val generation = when (event.data) {
                    is WatchPartyNavigateRequest -> null
                    else -> event.raw.getOrNull(1)?.toString()?.toLongOrNull()
                        ?: throw WatchPartyRequestException("Missing or invalid generation")
                }

                session.apply(event.data, connectionId, origin = id, expectedGeneration = generation)
                WatchPartyAcknowledgement(session.state.snapshot(connectionId))
            } catch (cause: WatchPartyRequestException) {
                WatchPartyAcknowledgement(session.state.snapshot(connectionId), cause.message)
            }

            event.acknowledge { result }
        }
    }
}
