package de.mineking.hexo.watchparty.client

import de.mineking.hexo.utils.socketio.client.SocketIOClient
import de.mineking.hexo.utils.socketio.client.awaitConnect
import de.mineking.hexo.watchparty.model.WatchPartyConnectionId
import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.protocol.WatchPartyClosedResponse
import de.mineking.hexo.watchparty.protocol.WatchPartyConnectData
import de.mineking.hexo.watchparty.protocol.WatchPartyCreatedResponse
import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import de.mineking.hexo.watchparty.protocol.WatchPartyRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.post
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json

expect val DefaultHttpEngine: HttpClientEngine
private val logger = KotlinLogging.logger {}
private val json = Json { allowStructuredMapKeys = true }

fun createDefaultHttpClient(
    engine: HttpClientEngine = DefaultHttpEngine,
    config: HttpClientConfig<*>.() -> Unit = {},
) = HttpClient(engine) {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(json)
    }

    install(ContentNegotiation) {
        json(json)
    }

    config()
}

class WatchPartyClient(
    internal val host: String,
    private val httpClient: HttpClient = createDefaultHttpClient(),
    private val coroutineScope: CoroutineScope,
) {
    suspend fun createWatchParty(): WatchPartyId {
        val response = httpClient.post("${host.trimEnd('/')}/api/watchparties")
        check(response.status.isSuccess()) { "Failed to create watchparty: ${response.status}" }

        return response.body<WatchPartyCreatedResponse>().id
    }

    suspend fun connectWatchParty(
        id: WatchPartyId,
        detachOnClose: Boolean,
        connectionId: WatchPartyConnectionId? = null,
    ): WatchParty? {
        val socket = SocketIOClient<WatchPartyResponse, WatchPartyRequest>(
            client = httpClient,
            url = Url("${host.trimEnd('/')}/api/watchparties"),
            connectionData = WatchPartyConnectData(
                watchPartyId = id,
                connectionId = connectionId ?: WatchPartyConnectionId.generate(),
                detachOnClose = detachOnClose,
            ),
            format = json,
        )

        val lock = SynchronizedObject()
        val lifetime = SupervisorJob(coroutineScope.coroutineContext[Job])

        val initial = CompletableDeferred<WatchParty>(lifetime)
        var watchParty: WatchParty? = null

        lifetime.invokeOnCompletion {
            socket.disconnect()
            synchronized(lock) { watchParty }?.onClosed(WatchPartyCloseReason(closedByServer = false))
        }

        socket.listen<WatchPartyDto> { event ->
            synchronized(lock) {
                if (!lifetime.isActive) return@listen
                val party = watchParty ?: WatchParty(this, socket, event.data).also {
                    watchParty = it
                    it.onClose { lifetime.cancel() }
                }
                party.onData(event.data)
                initial.complete(party)
            }
        }

        socket.listen<WatchPartyClosedResponse> { event ->
            initial.completeExceptionally(WatchPartyRequestException(event.data.message))
            synchronized(lock) { watchParty }?.onClosed(WatchPartyCloseReason(closedByServer = true))
            lifetime.cancel()
        }

        @Suppress("TooGenericExceptionCaught")
        try {
            socket.awaitConnect()
            return initial.await()
        } catch (cause: CancellationException) {
            lifetime.cancel()
            throw cause
        } catch (cause: Exception) {
            lifetime.cancel()
            currentCoroutineContext().ensureActive()
            logger.warn(cause) { "Failed to connect to watchparty ${id.value}" }
            return null
        }
    }
}

suspend fun WatchPartyClient.createAndConnectWatchParty(detachOnClose: Boolean, connectionId: WatchPartyConnectionId? = null): WatchParty {
    val id = createWatchParty()
    val watchParty = connectWatchParty(id, detachOnClose, connectionId)

    return checkNotNull(watchParty) { "Could not connect to the created watchparty" }
}
