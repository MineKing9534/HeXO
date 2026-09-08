package de.mineking.hexo.watchparty.client

import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.protocol.WatchPartyAcceptedResponse
import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import de.mineking.hexo.watchparty.protocol.WatchPartyErrorResponse
import de.mineking.hexo.watchparty.protocol.WatchPartyPingRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyPongResponse
import de.mineking.hexo.watchparty.protocol.WatchPartyRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyResponse
import de.mineking.hexo.watchparty.protocol.WatchPartyWebsocketCodes
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.converter
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.parameter
import io.ktor.serialization.deserialize
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

expect val DefaultHttpEngine: HttpClientEngine

private val logger = KotlinLogging.logger {}

fun createDefaultHttpClient(
    engine: HttpClientEngine = DefaultHttpEngine,
    config: HttpClientConfig<*>.() -> Unit = {},
) = HttpClient(engine) {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json {
            allowStructuredMapKeys = true
        })
    }

    config()
}

class WatchPartyClient(
    internal val host: String,
    private val httpClient: HttpClient = createDefaultHttpClient(),
    private val coroutineScope: CoroutineScope,
) {
    private sealed interface ConnectResult {
        data class Connected(
            val session: DefaultClientWebSocketSession,
            val initial: WatchPartyDto,
        ) : ConnectResult

        data object NotFound : ConnectResult
        data object Failed : ConnectResult
    }

    private suspend fun connect(
        id: WatchPartyId?,
        detachOnClose: Boolean,
        connectionId: String,
    ): ConnectResult {
        var openedSession: DefaultClientWebSocketSession? = null
        var connected = false
        @Suppress("TooGenericExceptionCaught")
        try {
            val session = httpClient.webSocketSession("${host.replace("http", "ws")}/api/watchparty/connect") {
                parameter("id", id?.value)
                parameter("detachOnClose", detachOnClose)
                parameter("connectionId", connectionId)
            }.also { openedSession = it }

            val frame = session.incoming.receive()
            if (frame is Frame.Close) {
                val reason = frame.readReason()
                return when (reason?.code) {
                    WatchPartyWebsocketCodes.NotFound -> ConnectResult.NotFound
                    else -> {
                        logger.warn { "WatchParty closed unexpectedly: $reason" }
                        ConnectResult.Failed
                    }
                }
            }

            val initial = session.converter!!.deserialize<WatchPartyResponse>(frame)
            check(initial is WatchPartyDto)
            connected = true
            return ConnectResult.Connected(session, initial)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.error(e) { "Failed to connect to watch party" }
            return ConnectResult.Failed
        } finally {
            if (!connected) openedSession?.cancel()
        }
    }

    suspend fun connectWatchParty(
        id: WatchPartyId?,
        detachOnClose: Boolean,
        connectionId: String? = null,
    ): WatchParty? {
        val stableConnectionId = connectionId ?: Uuid.random().toString()
        var connection = connect(id, detachOnClose, stableConnectionId) as? ConnectResult.Connected ?: return null
        val watchParty = WatchParty(this, connection.session, connection.initial)
        val reconnectId = watchParty.id

        coroutineScope.launch {
            while (!watchParty.isClosed) {
                try {
                    handleConnectionFrames(connection.session, watchParty)
                } finally {
                    withContext(NonCancellable) { connection.session.cancel() }
                    watchParty.onDisconnected()
                }

                if (watchParty.isClosed) break

                var replacement: ConnectResult.Connected? = null
                while (replacement == null && !watchParty.isClosed) {
                    delay(RECONNECT_DELAY)
                    when (val result = connect(reconnectId, detachOnClose, stableConnectionId)) {
                        is ConnectResult.Connected -> replacement = result
                        is ConnectResult.NotFound -> {
                            watchParty.onClosed(WatchPartyCloseReason(closedByServer = true))
                            return@launch
                        }
                        is ConnectResult.Failed -> {}
                    }
                }

                if (watchParty.isClosed) {
                    replacement?.session?.close()
                    break
                }

                connection = checkNotNull(replacement)
                if (!watchParty.onReconnected(connection.session, connection.initial)) {
                    connection.session.cancel()
                }
            }
        }

        return watchParty
    }

    private suspend fun handleConnectionFrames(session: DefaultClientWebSocketSession, watchParty: WatchParty) {
        @Suppress("TooGenericExceptionCaught")
        try {
            coroutineScope {
                val pongs = Channel<Unit>(Channel.CONFLATED)
                val heartbeat = launch {
                    while (!watchParty.isClosed) {
                        session.sendSerialized<WatchPartyRequest>(WatchPartyPingRequest)
                        if (withTimeoutOrNull(HEARTBEAT_TIMEOUT) { pongs.receive() } == null) {
                            watchParty.onDisconnected()
                            error("WatchParty heartbeat timed out")
                        }

                        delay(HEARTBEAT_INTERVAL)
                    }
                }

                try {
                    for (frame in session.incoming) {
                        if (frame is Frame.Close) return@coroutineScope

                        when (val response = session.converter!!.deserialize<WatchPartyResponse>(frame)) {
                            is WatchPartyDto -> watchParty.onData(response)
                            is WatchPartyErrorResponse -> watchParty.onRejected(response.requestId, response.message, response.state)
                            is WatchPartyAcceptedResponse -> watchParty.onAccepted(response.requestId, response.revision)
                            is WatchPartyPongResponse -> pongs.trySend(Unit)
                        }
                    }
                } finally {
                    heartbeat.cancel()
                    pongs.close()
                }
            }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            if (!watchParty.isClosed) logger.warn(e) { "WatchParty connection lost" }
        }
    }

    private companion object {
        val HEARTBEAT_INTERVAL = 5.seconds
        val HEARTBEAT_TIMEOUT = 1.seconds
        val RECONNECT_DELAY = 2.seconds
    }
}

suspend fun WatchPartyClient.createWatchParty(detachOnClose: Boolean, connectionId: String? = null) =
    connectWatchParty(null, detachOnClose, connectionId)!!
