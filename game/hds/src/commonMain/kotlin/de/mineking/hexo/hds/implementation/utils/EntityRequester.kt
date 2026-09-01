package de.mineking.hexo.hds.implementation.utils

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
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

suspend inline fun <reified D, T> HttpResponse.parseBodyOrNull(parse: (D) -> T) = when {
    status.isSuccess() -> parse(body())
    status == HttpStatusCode.NotFound -> null
    else -> throw EntityRequestException(bodyAsText())
}
