package de.mineking.hexo.sync.client

import de.mineking.hexo.sync.common.WatchPartyData
import de.mineking.hexo.sync.common.WatchPartyErrorResponse
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.sync.common.WatchPartyResponse
import de.mineking.hexo.sync.common.WatchPartyWebsocketCodes
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.converter
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.parameter
import io.ktor.serialization.deserialize
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

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
    private val host: String,
    private val httpClient: HttpClient = createDefaultHttpClient(),
) {
    suspend fun connectWatchParty(
        id: WatchPartyId?,
        detachOnClose: Boolean,
        connectionId: String? = null,
    ): WatchParty? {
        val wsSession = try {
            httpClient.webSocketSession("${host.replace("http", "ws")}/api/watchparties/ws") {
                parameter("id", id?.value)
                parameter("detachOnClose", detachOnClose)
                parameter("connectionId", connectionId)
            }
        } catch (e: WebSocketException) {
            logger.error(e) { "Failed to connect to watch party" }
            return null
        }

        val frame = wsSession.incoming.receive()
        if (frame is Frame.Close) {
            val reason = frame.readReason()
            if (reason?.code != WatchPartyWebsocketCodes.NotFound) {
                logger.warn { "WatchParty closed unexpectedly: $reason" }
            }

            return null
        }

        val initial = wsSession.converter!!.deserialize<WatchPartyResponse>(frame)
        check(initial is WatchPartyData)

        val flow = MutableStateFlow(initial)

        val session = WatchParty(flow, wsSession)

        wsSession.launch {
            try {
                for (frame in wsSession.incoming) {
                    if (frame is Frame.Close) return@launch

                    when (val response = wsSession.converter!!.deserialize<WatchPartyResponse>(frame)) {
                        is WatchPartyData -> flow.value = response
                        is WatchPartyErrorResponse -> logger.error { response.message }
                    }
                }
            } finally {
                val closedByServer = wsSession.closeReason.await()?.knownReason != CloseReason.Codes.GOING_AWAY
                session.onClose(WatchPartyCloseReason(closedByServer = closedByServer))
            }
        }

        return session
    }
}

suspend fun WatchPartyClient.createSession(detachOnClose: Boolean, connectionId: String? = null) =
    connectWatchParty(null, detachOnClose, connectionId)!!
