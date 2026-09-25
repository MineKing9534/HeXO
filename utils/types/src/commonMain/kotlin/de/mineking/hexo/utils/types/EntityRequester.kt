package de.mineking.hexo.utils.types

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface EntityRequester<K, T> {
    suspend fun fetch(id: K): T
}

interface EntityRequesterFactory {
    fun <K, T> createEntityRequester(resolver: suspend (K) -> T): EntityRequester<K, T>

    class Debouncing(private val coroutineScope: CoroutineScope) : EntityRequesterFactory {
        override fun <K, T> createEntityRequester(resolver: suspend (K) -> T) = DebouncingEntityRequester(coroutineScope, resolver)
    }
}

class DebouncingEntityRequester<K, T>(
    private val coroutineScope: CoroutineScope,
    private val request: suspend (K) -> T,
) : EntityRequester<K, T> {
    private val waitingLock = Mutex()
    private val waiting = mutableMapOf<K, Deferred<T>>()

    override suspend fun fetch(id: K): T {
        val deferred = waitingLock.withLock {
            waiting.getOrPut(id) {
                coroutineScope.async {
                    request(id)
                }
            }
        }

        return try {
            deferred.await()
        } finally {
            waitingLock.withLock {
                if (waiting[id] === deferred) {
                    waiting -= id
                }
            }
        }
    }
}

class EntityRequestException(override val message: String) : RuntimeException(message)
