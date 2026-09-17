package de.mineking.hexo.utils.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class CacheTest {
    @Test
    fun `supports basic cache operations`() = runTest {
        val cache = cache<String, Int>()

        assertNull(cache.getIfAvailable("a"))
        assertNull(cache.put("a", 1))

        assertEquals(1, cache.get("a"))
        assertTrue(cache.containsKey("a"))

        assertEquals(1, cache.put("a", 2))
        assertEquals(2, cache.getIfAvailable("a"))

        assertEquals(2, cache.remove("a"))
        assertNull(cache.remove("a"))
        assertFalse(cache.containsKey("a"))
    }

    @Test
    @Suppress("StatementWrapping")
    fun `supports nullable cached values`() = runTest {
        val cache = cache<String, String?>()
        var calculations = 0

        assertNull(cache.getOrPut("a") { calculations++; null })
        assertNull(cache.getOrPut("a") { calculations++; "unexpected" })
        assertEquals(1, calculations)
        assertTrue(cache.containsKey("a"))
    }

    @Test
    fun `weighted size limit evicts the configured entry`() = runTest {
        val cache = InMemoryCache(
            CacheConfiguration<String, String>(
                sizeLimit = SizeLimit(5) { it.length.toLong() },
                evictionStrategy = EvictionStrategy.FirstInFirstOut,
                expiration = null,
            ),
        )

        cache.put("a", "aaa")
        cache.put("b", "bbb")

        assertNull(cache.getIfAvailable("a"))
        assertEquals("bbb", cache.getIfAvailable("b"))
    }

    @Test
    fun `first in first out eviction does not depend on wall clock order`() = runTest {
        val clock = TestClock()
        clock.advance(100)
        val cache = cache<String, Int>(size = 2, strategy = EvictionStrategy.FirstInFirstOut, clock = clock)

        cache.put("a", 1)
        clock.advance(-100)
        cache.put("b", 2)
        cache.put("c", 3)

        assertNull(cache.getIfAvailable("a"))
        assertEquals(2, cache.getIfAvailable("b"))
        assertEquals(3, cache.getIfAvailable("c"))
    }

    @Test
    fun `least recently used eviction considers reads`() = runTest {
        val cache = cache<String, Int>(size = 2, strategy = EvictionStrategy.LeastRecentlyUsed)

        cache.put("a", 1)
        cache.put("b", 2)
        val _ = cache.get("a")
        cache.put("c", 3)

        assertEquals(1, cache.getIfAvailable("a"))
        assertNull(cache.getIfAvailable("b"))
    }

    @Test
    fun `least frequently used eviction considers access count`() = runTest {
        val cache = cache<String, Int>(size = 2, strategy = EvictionStrategy.LeastFrequentlyUsed)

        cache.put("a", 1)
        cache.put("b", 2)
        repeat(2) { val _ = cache.get("a") }
        cache.put("c", 3)

        assertEquals(1, cache.getIfAvailable("a"))
        assertNull(cache.getIfAvailable("b"))
    }

    @Test
    fun `expiration strategies are applied`() = runTest {
        val clock = TestClock()
        val afterWrite = cache<String, Int>(expiration = ExpirationStrategy.AfterWrite(20.milliseconds), clock = clock)

        afterWrite.put("a", 1)
        clock.advance(30)
        assertNull(afterWrite.getIfAvailable("a"))

        val afterAccess = cache<String, Int>(expiration = ExpirationStrategy.AfterAccess(40.milliseconds), clock = clock)
        afterAccess.put("a", 1)
        clock.advance(25)
        assertEquals(1, afterAccess.get("a"))
        clock.advance(25)
        assertEquals(1, afterAccess.getIfAvailable("a"))
    }

    @Test
    fun `concurrent misses share one calculation`() = runTest {
        val cache = cache<String, Int>()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calculations = 0

        val results = List(10) {
            async {
                cache.getOrPut("a") {
                    calculations++
                    started.complete(Unit)
                    release.await()
                    42
                }
            }
        }

        started.await()
        release.complete(Unit)

        assertEquals(List(10) { 42 }, results.awaitAll())
        assertEquals(1, calculations)
    }

    @Test
    fun `get awaits an in-flight calculation while getIfAvailable does not`() = runTest {
        val cache = cache<String, Int>()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        val calculation = async {
            cache.getOrPut("a") {
                started.complete(Unit)
                release.await()
                42
            }
        }

        started.await()
        val get = async { cache.get("a") }

        assertFalse(get.isCompleted)
        assertNull(cache.getIfAvailable("a"))

        release.complete(Unit)

        assertEquals(42, calculation.await())
        assertEquals(42, get.await())
    }

    @Test
    fun `creation exceptions are shared and rethrown but not cached`() = runTest {
        val cache = cache<String, Int>()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calculations = 0

        val results = List(10) {
            async {
                runCatching {
                    cache.getOrPut("a") {
                        calculations++
                        started.complete(Unit)
                        release.await()
                        throw TestException()
                    }
                }
            }
        }

        started.await()
        release.complete(Unit)
        results.awaitAll().forEach { result -> assertFailsWith<TestException> { result.getOrThrow() } }

        assertEquals(1, calculations)
        assertNull(cache.getIfAvailable("a"))
    }

    private fun <K, V> cache(
        size: Long = Long.MAX_VALUE,
        strategy: EvictionStrategy<K, V> = EvictionStrategy.LeastRecentlyUsed,
        expiration: ExpirationStrategy<K, V>? = null,
        clock: Clock = Clock.System,
    ) = InMemoryCache(
        configuration = CacheConfiguration(size.entries, strategy, expiration),
        clock = clock,
    )

    private class TestClock : Clock {
        private var instant = Instant.fromEpochMilliseconds(0)
        override fun now() = instant
        fun advance(milliseconds: Long) {
            instant += milliseconds.milliseconds
        }
    }

    private class TestException : RuntimeException()
}
