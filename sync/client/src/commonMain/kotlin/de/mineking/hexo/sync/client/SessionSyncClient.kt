package de.mineking.hexo.sync.client

import de.mineking.hexo.sync.common.SessionSyncData
import de.mineking.hexo.sync.common.SessionSyncId
import de.mineking.hexo.sync.common.SessionSyncWebsocketCodes
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.converter
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.parameter
import io.ktor.serialization.deserialize
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
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
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    config()
}

class SessionSyncClient(
    private val host: String,
    private val httpClient: HttpClient = createDefaultHttpClient(),
) {
    suspend fun connectSession(id: SessionSyncId?): SyncSession? {
        val wsSession = httpClient.webSocketSession("$host/api/sessions/sync/gateway") {
            parameter("id", id?.value)
        }

        val frame = wsSession.incoming.receive()
        if (frame is Frame.Close) {
            val reason = frame.readReason()
            if (reason?.code != SessionSyncWebsocketCodes.NotFound) {
                logger.warn { "SessionSync closed unexpectedly: $reason" }
            }

            return null
        }

        val initial = wsSession.converter!!.deserialize<SessionSyncData>(frame)
        val flow = MutableStateFlow(initial)

        val session = SyncSession(flow, wsSession)
        wsSession.launch {
            for (frame in wsSession.incoming) {
                if (frame is Frame.Close) return@launch

                flow.value = wsSession.converter!!.deserialize<SessionSyncData>(frame)
            }
        }

        return session
    }
}

suspend fun SessionSyncClient.createSession() = connectSession(null)!!
