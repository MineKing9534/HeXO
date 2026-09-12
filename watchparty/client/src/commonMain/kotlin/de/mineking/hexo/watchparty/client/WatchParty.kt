package de.mineking.hexo.watchparty.client

import de.mineking.hexo.utils.socketio.client.SocketIOClient
import de.mineking.hexo.utils.types.Entity
import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.model.WatchPartyNavigateTarget
import de.mineking.hexo.watchparty.protocol.WatchPartyAcknowledgement
import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import de.mineking.hexo.watchparty.protocol.WatchPartyNavigateRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyResponse
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class WatchPartyCloseReason(val closedByServer: Boolean)
class WatchPartyRequestException(message: String, cause: Throwable? = null) : Exception(message, cause)

class WatchParty internal constructor(
    private val client: WatchPartyClient,
    val socket: SocketIOClient<WatchPartyResponse, WatchPartyRequest>,
    data: WatchPartyDto,
) : Entity<WatchPartyId> {
    val connected = socket.connected

    private val onClose = mutableListOf<(WatchPartyCloseReason) -> Unit>()
    private val stateLock = SynchronizedObject()
    private var closed = false
    private val confirmedState = ConfirmedWatchPartyState(data)

    val target: StateFlow<WatchPartyTarget?>
        field = MutableStateFlow(AbstractWatchPartyTargetImpl.of(this, data))

    override val id = data.id
    override val url get() = "${client.host}/watchparty/${id.value}"

    internal fun onClosed(reason: WatchPartyCloseReason) {
        val callbacks = synchronized(stateLock) {
            if (closed) return
            closed = true
            onClose.toList().also { onClose.clear() }
        }
        socket.disconnect()
        callbacks.forEach { it(reason) }
    }

    internal fun onData(data: WatchPartyDto) {
        synchronized(stateLock) {
            if (closed) return
            if (confirmedState.update(data)) {
                target.value = AbstractWatchPartyTargetImpl.of(this, confirmedState.data)
            }
        }
    }

    @Suppress("ThrowsCount")
    internal suspend fun request(action: WatchPartyRequest, generation: Long?) {
        synchronized(stateLock) {
            if (!socket.connected.value || closed) {
                throw WatchPartyRequestException("Watch party is not connected")
            }
        }

        @Suppress("TooGenericExceptionCaught")
        try {
            val ack = socket.requestAwait<WatchPartyAcknowledgement>(action, generation?.toString() ?: "")
            onData(ack.state)

            ack.error?.let { throw WatchPartyRequestException(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: WatchPartyRequestException) {
            throw e
        } catch (error: Exception) {
            throw WatchPartyRequestException("Watch party request failed", error)
        }
    }

    suspend fun navigate(target: WatchPartyNavigateTarget?) {
        request(WatchPartyNavigateRequest(target), generation = null)
    }

    fun onClose(block: (WatchPartyCloseReason) -> Unit) {
        synchronized(stateLock) { onClose += block }
    }

    fun close() {
        onClosed(WatchPartyCloseReason(closedByServer = false))
    }
}
