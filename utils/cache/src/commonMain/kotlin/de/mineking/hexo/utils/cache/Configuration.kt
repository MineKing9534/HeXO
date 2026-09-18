package de.mineking.hexo.utils.cache

import kotlin.time.Duration
import kotlin.time.Instant

interface CacheEntry<out K, out V> {
    val key: K
    val value: V
    val accessCount: Long
    val accessOrder: Long get() = insertionOrder
    val accessedAt: Instant
    val writtenAt: Instant
    val insertionOrder: Long
    val size: Long
}

interface EvictionStrategy<in K, in V> {
    val comparator: Comparator<in CacheEntry<K, V>>

    object FirstInFirstOut : EvictionStrategy<Any?, Any?> {
        override val comparator = compareBy<CacheEntry<Any?, Any?>> { it.insertionOrder }
    }

    object LeastRecentlyUsed : EvictionStrategy<Any?, Any?> {
        override val comparator = compareBy<CacheEntry<Any?, Any?>> { it.accessOrder }
    }

    object LeastFrequentlyUsed : EvictionStrategy<Any?, Any?> {
        override val comparator = compareBy<CacheEntry<Any?, Any?>> { it.accessCount }
    }
}

@Suppress("FunctionNaming")
fun <K, V> EvictionStrategy(comparator: Comparator<in CacheEntry<K, V>>) = object : EvictionStrategy<K, V> {
    override val comparator = comparator
}

interface ExpirationStrategy<in K, in V> {
    fun isExpired(now: Instant, entry: CacheEntry<K, V>): Boolean

    class AfterWrite(val duration: Duration) : ExpirationStrategy<Any?, Any?> {
        init {
            require(duration >= Duration.ZERO) { "Expiration duration must not be negative" }
        }

        override fun isExpired(now: Instant, entry: CacheEntry<Any?, Any?>) = now - entry.writtenAt >= duration
    }

    class AfterAccess(val duration: Duration) : ExpirationStrategy<Any?, Any?> {
        init {
            require(duration >= Duration.ZERO) { "Expiration duration must not be negative" }
        }

        override fun isExpired(now: Instant, entry: CacheEntry<Any?, Any?>) = now - entry.accessedAt >= duration
    }
}

data class SizeLimit<in T>(val limit: Long, val calculator: SizeCalculator<T>) {
    init {
        require(limit >= 0) { "Size limit must not be negative" }
    }
}

fun interface SizeCalculator<in T> {
    fun size(value: T): Long

    object Entries : SizeCalculator<Any?> {
        override fun size(value: Any?) = 1L
    }

    object Bytes : SizeCalculator<ByteArray> {
        override fun size(value: ByteArray) = value.size.toLong()
    }
}

val Long.entries get() = SizeLimit(this, SizeCalculator.Entries)
val Int.entries get() = toLong().entries
val Long.bytes get() = SizeLimit(this, SizeCalculator.Bytes)
val Int.megabytes get() = (this * 1024 * 1024L).bytes

data class CacheConfiguration<in K, in V>(
    val sizeLimit: SizeLimit<V>?,
    val evictionStrategy: EvictionStrategy<K, V>,
    val expiration: ExpirationStrategy<K, V>?,
)
