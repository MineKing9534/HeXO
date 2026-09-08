package de.mineking.hexo.watchparty.client

import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class ConfirmedWatchPartyState(initial: WatchPartyDto) {
    private val lock = SynchronizedObject()
    private var current = initial
    val data get() = synchronized(lock) { current }

    fun update(next: WatchPartyDto): Boolean = synchronized(lock) {
        if (next.id != current.id || next.revision < current.revision) return false
        current = next
        true
    }
}
