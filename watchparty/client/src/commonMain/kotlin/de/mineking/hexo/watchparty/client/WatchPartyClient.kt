package de.mineking.hexo.watchparty.client

import de.mineking.hexo.watchparty.common.WatchPartyData
import de.mineking.hexo.watchparty.common.WatchPartyErrorResponse
import de.mineking.hexo.watchparty.common.WatchPartyId
import de.mineking.hexo.watchparty.common.WatchPartyPingRequest
import de.mineking.hexo.watchparty.common.WatchPartyPongResponse
import de.mineking.hexo.watchparty.common.WatchPartyRequest
import de.mineking.hexo.watchparty.common.WatchPartyResponse
import de.mineking.hexo.watchparty.common.WatchPartyWebsocketCodes
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

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
            val initial: WatchPartyData,
        ) : ConnectResult

        data object NotFound : ConnectResult
        data object Failed : ConnectResult
    }

    private suspend fun connect(
        id: WatchPartyId?,
        detachOnClose: Boolean,
        connectionId: String?,
    ): ConnectResult {
        @Suppress("TooGenericExceptionCaught")
        try {
            val session = httpClient.webSocketSession("${host.replace("http", "ws")}/api/watchparties/ws") {
                parameter("id", id?.value)
                parameter("detachOnClose", detachOnClose)
                parameter("connectionId", connectionId)
            }

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
            check(initial is WatchPartyData)
            return ConnectResult.Connected(session, initial)
        } catch (e: Exception) {
            logger.error(e) { "Failed to connect to watch party" }
            return ConnectResult.Failed
        }
    }

    suspend fun connectWatchParty(
        id: WatchPartyId?,
        detachOnClose: Boolean,
        connectionId: String? = null,
    ): WatchParty? {
        var connection = connect(id, detachOnClose, connectionId) as? ConnectResult.Connected ?: return null
        val watchParty = WatchParty(this, connection.session, connection.initial)
        val reconnectId = watchParty.id

        coroutineScope.launch {
            while (!watchParty.isClosed) {
                handleConnectionFrames(connection.session, watchParty)

                if (watchParty.isClosed) break
                watchParty.onDisconnected()

                var replacement: ConnectResult.Connected? = null
                while (replacement == null && !watchParty.isClosed) {
                    delay(RECONNECT_DELAY)
                    when (val result = connect(reconnectId, detachOnClose, connectionId)) {
                        is ConnectResult.Connected -> replacement = result
                        is ConnectResult.NotFound -> {
                            watchParty.onClosed(WatchPartyCloseReason(closedByServer = true))
                            return@launch
                        }
                        is ConnectResult.Failed -> Unit
                    }
                }

                if (watchParty.isClosed) {
                    replacement?.session?.close()
                    break
                }

                connection = checkNotNull(replacement)
                watchParty.onReconnected(connection.session, connection.initial)
            }
        }

        return watchParty
    }

    private suspend fun handleConnectionFrames(session: DefaultClientWebSocketSession, watchParty: WatchParty) {
        @Suppress("TooGenericExceptionCaught")
        try {
            coroutineScope {
                val pongs = Channel<Unit>(Channel.CONFLATED)
                launch {
                    while (!watchParty.isClosed) {
                        session.sendSerialized<WatchPartyRequest>(WatchPartyPingRequest)
                        if (withTimeoutOrNull(HEARTBEAT_TIMEOUT) { pongs.receive() } == null) {
                            watchParty.onDisconnected()
                            error("WatchParty heartbeat timed out")
                        }

                        delay(HEARTBEAT_INTERVAL)
                    }
                }

                for (frame in session.incoming) {
                    if (frame is Frame.Close) return@coroutineScope

                    when (val response = session.converter!!.deserialize<WatchPartyResponse>(frame)) {
                        is WatchPartyData -> watchParty.onData(response)
                        is WatchPartyErrorResponse -> logger.error { response.message }
                        is WatchPartyPongResponse -> pongs.trySend(Unit)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!watchParty.isClosed) logger.warn(e) { "WatchParty connection lost" }
        }
    }

    private companion object {
        val HEARTBEAT_INTERVAL = 1.seconds
        val HEARTBEAT_TIMEOUT = 1.seconds
        val RECONNECT_DELAY = 2.seconds
    }
}

suspend fun WatchPartyClient.createWatchParty(detachOnClose: Boolean, connectionId: String? = null) =
    connectWatchParty(null, detachOnClose, connectionId)!!
