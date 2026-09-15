package de.mineking.hexo.utils.coroutines

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

class KeyedSemaphore<K>(private val permits: Int = 1) {
    private class Entry(val semaphore: Semaphore, var references: Int)

    init {
        require(permits > 0) { "permits must be positive" }
    }

    private val entriesLock = Mutex()
    private val entries = mutableMapOf<K, Entry>()

    suspend fun <T> withPermit(key: K, action: suspend () -> T): T {
        val entry = entriesLock.withLock {
            entries.getOrPut(key) { Entry(Semaphore(permits), 0) }
                .also { it.references++ }
        }

        try {
            return entry.semaphore.withPermit { action() }
        } finally {
            entriesLock.withLock {
                entry.references--
                if (entry.references == 0) entries.remove(key)
            }
        }
    }
}
