package de.mineking.hexo.watchparty.server

import de.mineking.hexo.utils.socketio.server.SocketId
import de.mineking.hexo.watchparty.model.WatchPartyConnectionId
import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.flow.MutableStateFlow

internal interface WatchPartyTargetContainer {
    var target: WatchPartyServerTarget?
    val generation: Long
}

internal class WatchPartyState(val id: WatchPartyId) {
    private data class Revision(val value: Long, val origin: SocketId?)

    private val lock = SynchronizedObject()
    private val revision = MutableStateFlow(Revision(0, null))
    private var target: WatchPartyServerTarget? = null
    private var generation = 0L

    private val container: WatchPartyTargetContainer = object : WatchPartyTargetContainer {
        override val generation get() = this@WatchPartyState.generation
        override var target: WatchPartyServerTarget?
            get() = this@WatchPartyState.target
            set(value) {
                this@WatchPartyState.target = value
                this@WatchPartyState.generation++
            }
    }

    fun update(
        connectionId: WatchPartyConnectionId,
        origin: SocketId? = null,
        block: context(WatchPartyConnectionId) WatchPartyTargetContainer.() -> Unit,
    ) = synchronized(lock) {
        context(connectionId) {
            try {
                container.block()
                revision.value = Revision(revision.value.value + 1, origin)
            } catch (error: WatchPartyRequestException) {
                throw WatchPartyRequestException(error.message, error)
            }
        }
    }

    suspend fun collect(
        connectionId: WatchPartyConnectionId,
        socketId: SocketId,
        block: suspend (WatchPartyDto) -> Unit,
    ): Nothing {
        revision.collect { revision ->
            if (revision.origin != socketId) block(snapshot(connectionId))
        }
    }

    fun snapshot(connectionId: WatchPartyConnectionId) = synchronized(lock) {
        context(connectionId) {
            toDto()
        }
    }

    context(connectionId: WatchPartyConnectionId)
    private fun toDto() = WatchPartyDto(
        id = id,
        revision = revision.value.value,
        generation = generation,
        target = target?.toDto(),
        clearableHighlights = target?.hasClearableHighlights() ?: false,
    )
}
