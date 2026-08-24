package de.mineking.hexo.hds.implementation.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface EntityRequester<K, T> {
    suspend fun fetch(id: K): T?
}

interface EntityRequesterFactory {
    fun <K, T> createEntityRequester(resolver: suspend (K) -> T?): EntityRequester<K, T>

    class Debouncing(private val coroutineScope: CoroutineScope) : EntityRequesterFactory {
        override fun <K, T> createEntityRequester(resolver: suspend (K) -> T?) = DebouncingEntityRequester(coroutineScope, resolver)
    }
}

fun EntityRequesterFactory.logRequestErrors(): EntityRequesterFactory = ErrorLoggingEntityRequesterFactory(this)
private class ErrorLoggingEntityRequesterFactory(val delegate: EntityRequesterFactory) : EntityRequesterFactory {
    private val logger = KotlinLogging.logger {}

    override fun <K, T> createEntityRequester(resolver: suspend (K) -> T?) = delegate.createEntityRequester<K, T> {
        @Suppress("TooGenericExceptionCaught")
        try {
            resolver(it)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error during entity request for $it" }
            null
        }
    }
}

class DebouncingEntityRequester<K, T>(
    private val coroutineScope: CoroutineScope,
    private val request: suspend (K) -> T?,
) : EntityRequester<K, T> {
    private val waitingLock = Mutex()
    private val waiting = mutableMapOf<K, Deferred<T?>>()

    override suspend fun fetch(id: K): T? {
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
