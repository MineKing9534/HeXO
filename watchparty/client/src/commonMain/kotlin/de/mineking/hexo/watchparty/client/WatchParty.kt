package de.mineking.hexo.watchparty.client

import de.mineking.hexo.utils.types.Entity
import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.model.WatchPartyNavigateTarget
import de.mineking.hexo.watchparty.protocol.WatchPartyAction
import de.mineking.hexo.watchparty.protocol.WatchPartyActionRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import de.mineking.hexo.watchparty.protocol.WatchPartyNavigateRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyRequestId
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.close
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

data class WatchPartyCloseReason(val closedByServer: Boolean)

class WatchParty internal constructor(
    private val client: WatchPartyClient,
    private var wsSession: DefaultClientWebSocketSession,
    data: WatchPartyDto,
) : Entity<WatchPartyId> {
    private val onClose = mutableListOf<(WatchPartyCloseReason) -> Unit>()
    private val stateLock = SynchronizedObject()
    private var closed = false
    private val confirmedState = ConfirmedWatchPartyState(data)
    private val pendingRequests = PendingWatchPartyRequests(data.revision)

    private fun reconcile() = synchronized(stateLock) {
        target.value = AbstractWatchPartyTargetImpl.of(this, confirmedState.data)
        pendingRequests.confirmState(confirmedState.data.revision)
    }

    internal fun onAccepted(id: WatchPartyRequestId, revision: Long) {
        pendingRequests.accept(id, revision)
    }

    internal fun onRejected(id: WatchPartyRequestId?, message: String, state: WatchPartyDto) {
        synchronized(stateLock) {
            val _ = confirmedState.update(state)
            reconcile()
        }

        val error = WatchPartyRequestException(message)
        if (id == null) {
            pendingRequests.failAll(error)
        } else {
            pendingRequests.reject(id, error)
        }
    }

    val connected: StateFlow<Boolean>
        field = MutableStateFlow(true)

    val target: StateFlow<WatchPartyTarget?>
        field = MutableStateFlow(AbstractWatchPartyTargetImpl.of(this, data))

    internal val isClosed get() = synchronized(stateLock) { closed }

    override val id = data.id
    override val url get() = "${client.host}/watchparty/${id.value}"

    internal fun onClosed(reason: WatchPartyCloseReason) {
        val callbacks = synchronized(stateLock) {
            if (closed) return
            closed = true
            connected.value = false
            onClose.toList().also { onClose.clear() }
        }
        failPendingRequests("Watch party closed")
        callbacks.forEach { it(reason) }
    }

    internal fun onDisconnected() {
        synchronized(stateLock) { connected.value = false }
        failPendingRequests("Watch party disconnected")
    }

    private fun failPendingRequests(message: String) {
        reconcile()
        pendingRequests.failAll(WatchPartyRequestException(message))
    }

    internal fun onReconnected(session: DefaultClientWebSocketSession, initial: WatchPartyDto): Boolean = synchronized(stateLock) {
        if (closed) return false
        wsSession = session
        onData(initial)
        connected.value = true
        true
    }

    internal fun onData(data: WatchPartyDto) = synchronized(stateLock) {
        if (confirmedState.update(data)) {
            reconcile()
        }
    }

    internal suspend fun request(action: WatchPartyAction, generation: Long?) {
        val request = WatchPartyActionRequest(
            id = WatchPartyRequestId(Uuid.random().toString()),
            generation = generation,
            action = action,
        )

        val response = pendingRequests.register(request.id)
        var sentSession: DefaultClientWebSocketSession? = null

        @Suppress("TooGenericExceptionCaught")
        try {
            val session = synchronized(stateLock) {
                if (!connected.value || closed) throw WatchPartyRequestException("Watch party is not connected")
                wsSession
            }
            sentSession = session
            withTimeout(10.seconds) {
                session.sendSerialized<WatchPartyRequest>(request)
                response.await()
            }
        } catch (error: Exception) {
            @Suppress("InstanceOfCheckForException")
            if (error is TimeoutCancellationException || (error !is CancellationException && error !is WatchPartyRequestException)) {
                sentSession?.let {
                    invalidateConnection(it)
                }
            }
            reconcile()

            @Suppress("InstanceOfCheckForException")
            throw when (error) {
                is CancellationException if error !is TimeoutCancellationException -> error
                is WatchPartyRequestException -> error
                is TimeoutCancellationException -> WatchPartyRequestException("Watch party request timed out", error)
                else -> WatchPartyRequestException("Failed to send watch party request", error)
            }
        } finally {
            pendingRequests.discard(request.id)
        }
    }

    private fun invalidateConnection(session: DefaultClientWebSocketSession) {
        synchronized(stateLock) {
            if (wsSession !== session) return
            // An unacknowledged command may have been applied. Reconnect before accepting more edits.
            connected.value = false
            failPendingRequests("Watch party connection failed")
        }
        session.cancel()
    }

    suspend fun navigate(target: WatchPartyNavigateTarget?) {
        request(WatchPartyNavigateRequest(target), generation = null)
    }

    fun onClose(block: (WatchPartyCloseReason) -> Unit) {
        synchronized(stateLock) { onClose += block }
    }

    suspend fun close() {
        onClosed(WatchPartyCloseReason(closedByServer = false))
        val session = synchronized(stateLock) { wsSession }
        session.close()
    }
}
