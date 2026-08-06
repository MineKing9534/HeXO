package de.mineking.hexo.utils.coroutines

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

private suspend fun <A, B> createResult(finished: A?, waiting: Deferred<B?>): Pair<A, B>? {
    if (finished == null) {
        waiting.cancelAndJoin()
        return null
    }
    val secondResult = waiting.await() ?: return null
    return finished to secondResult
}

private fun <A, B> Pair<A, B>.reversed() = second to first

suspend fun <A, B> awaitBothOrNull(
    first: suspend () -> A?,
    second: suspend () -> B?,
) = coroutineScope {
    val firstJob = async { first() }
    val secondJob = async { second() }

    select<Pair<A, B>?> {
        firstJob.onAwait { first -> createResult(first, secondJob) }
        secondJob.onAwait { second -> createResult(second, firstJob)?.reversed() }
    }
}
