package de.mineking.hexo.watchparty.server

import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.flow.MutableStateFlow

internal interface WatchPartyTargetContainer {
    var target: WatchPartyServerTarget?
    val generation: Long
}

internal class WatchPartyState(val id: WatchPartyId) {
    private val lock = SynchronizedObject()
    private val revision = MutableStateFlow(0L)
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
        block: context(WatchPartyConnectionId) WatchPartyTargetContainer.() -> Unit,
    ): Long = synchronized(lock) {
        context(connectionId) {
            try {
                container.block()
                revision.value++
                revision.value
            } catch (error: WatchPartyRequestException) {
                throw WatchPartyRequestException(error.message, toDto(), error)
            }
        }
    }

    suspend fun collect(connectionId: WatchPartyConnectionId, block: suspend (WatchPartyDto) -> Unit): Nothing {
        revision.collect {
            block(snapshot(connectionId))
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
        revision = revision.value,
        generation = generation,
        target = target?.toDto(),
        clearableHighlights = target?.hasClearableHighlights() ?: false,
    )
}
