package de.mineking.hexo.utils.cache

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

interface Cache<K, V> {
    val configuration: CacheConfiguration<K, V>

    suspend fun get(key: K): V?
    fun getIfAvailable(key: K): V?

    suspend fun getOrPut(key: K, create: suspend (K) -> V): V

    @IgnorableReturnValue
    fun put(key: K, value: V): V?

    @IgnorableReturnValue
    fun remove(key: K): V?

    fun containsKey(key: K): Boolean
}

class InMemoryCache<K, V>(
    override val configuration: CacheConfiguration<K, V>,
    private val clock: Clock = Clock.System,
) : Cache<K, V> {
    private inner class Entry<K, V>(
        override val key: K,
        override var value: V,
        override var insertionOrder: Long,
        override var accessCount: Long,
        override var accessOrder: Long,
        override var accessedAt: Instant,
        override var writtenAt: Instant,
        override var size: Long,
    ) : CacheEntry<K, V> {
        fun recordAccess() {
            accessedAt = clock.now()
            accessOrder = nextAccessOrder()
            if (accessCount < Long.MAX_VALUE) accessCount++
        }
    }

    private val lock = SynchronizedObject()
    private val entries = mutableMapOf<K, Entry<K, V>>()
    private val inFlight = mutableMapOf<K, CompletableDeferred<V>>()
    private var currentSize = 0L
    private var nextInsertionOrder = 0L
    private var nextAccessOrder = 0L

    override suspend fun get(key: K): V? {
        val load = synchronized(lock) {
            removeExpired()
            entries[key]?.let {
                it.recordAccess()
                return it.value
            }
            inFlight[key]
        }
        return load?.await()
    }

    override fun getIfAvailable(key: K) = synchronized(lock) {
        removeExpired()
        entries[key]?.also { it.recordAccess() }?.value
    }

    override suspend fun getOrPut(key: K, create: suspend (K) -> V): V {
        var createsValue = false
        val deferred = synchronized(lock) {
            removeExpired()
            entries[key]?.let {
                it.recordAccess()
                return it.value
            }

            inFlight[key] ?: CompletableDeferred<V>().also {
                inFlight[key] = it
                createsValue = true
            }
        }

        if (!createsValue) return deferred.await()

        @Suppress("TooGenericExceptionCaught")
        return try {
            val value = create(key)
            synchronized(lock) {
                if (inFlight[key] === deferred) inFlight -= key
                putInternal(key, value)
            }
            deferred.complete(value)
            value
        } catch (throwable: Throwable) {
            withContext(NonCancellable) {
                synchronized(lock) {
                    if (inFlight[key] === deferred) inFlight -= key
                }
                deferred.completeExceptionally(throwable)
            }
            throw throwable
        }
    }

    override fun put(key: K, value: V) = synchronized(lock) {
        removeExpired()
        putInternal(key, value)
    }

    override fun remove(key: K) = synchronized(lock) {
        removeExpired()
        entries.remove(key)?.also { currentSize -= it.size }?.value
    }

    override fun containsKey(key: K) = synchronized(lock) {
        removeExpired()
        key in entries
    }

    private fun putInternal(key: K, value: V): V? {
        val now = clock.now()
        val valueSize = configuration.sizeLimit?.calculator?.size(value) ?: 0
        require(valueSize >= 0) { "Calculated cache entry size must not be negative" }

        val existing = entries[key]
        val previous = existing?.value
        if (existing != null) currentSize -= existing.size

        val limit = configuration.sizeLimit
        if (limit != null && valueSize > limit.limit) {
            entries.remove(key)
            return previous
        }

        if (existing == null) {
            entries[key] = Entry(key, value, nextInsertionOrder(), 0, nextAccessOrder(), now, now, valueSize)
        } else {
            existing.value = value
            existing.size = valueSize
            existing.writtenAt = now
            existing.accessedAt = now
            existing.accessCount = 0
            existing.accessOrder = nextAccessOrder()
        }

        currentSize = if (Long.MAX_VALUE - currentSize < valueSize) Long.MAX_VALUE else currentSize + valueSize
        evictEntries()
        return previous
    }

    private fun evictEntries() {
        val limit = configuration.sizeLimit ?: return
        while (currentSize > limit.limit) {
            val key = entries.values.minWithOrNull(configuration.evictionStrategy.comparator)?.key ?: return
            val removed = entries.remove(key) ?: continue
            currentSize -= removed.size
        }
    }

    private fun removeExpired() {
        val expiration = configuration.expiration ?: return
        val now = clock.now()

        entries.values
            .filter { expiration.isExpired(now, it) }
            .forEach { entry ->
                entries.remove(entry.key)
                currentSize -= entry.size
            }
    }

    private fun nextInsertionOrder(): Long {
        if (nextInsertionOrder == Long.MAX_VALUE) {
            entries.values
                .sortedBy { it.insertionOrder }
                .forEachIndexed { index, entry -> entry.insertionOrder = index.toLong() }
            nextInsertionOrder = entries.size.toLong()
        }

        return nextInsertionOrder++
    }

    private fun nextAccessOrder(): Long {
        if (nextAccessOrder == Long.MAX_VALUE) {
            entries.values
                .sortedBy { it.accessOrder }
                .forEachIndexed { index, entry -> entry.accessOrder = index.toLong() }
            nextAccessOrder = entries.size.toLong()
        }

        return nextAccessOrder++
    }
}
