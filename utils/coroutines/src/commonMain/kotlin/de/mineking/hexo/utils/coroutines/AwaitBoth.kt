package de.mineking.hexo.utils.coroutines

import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import de.mineking.hexo.utils.types.map
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

private suspend fun <A, B, E : IError> createResult(
    finished: Result<A, E>,
    waiting: Deferred<Result<B, E>>,
): Result<Pair<A, B>, E> {
    if (!finished.isSuccess()) {
        waiting.cancelAndJoin()
        return finished
    }

    return waiting.await()
        .map { finished.value to it }
}

private fun <A, B> Pair<A, B>.reversed() = second to first

suspend fun <A, B, E : IError> awaitBoth(
    first: suspend () -> Result<A, E>,
    second: suspend () -> Result<B, E>,
): Result<Pair<A, B>, E> = coroutineScope {
    val firstJob = async { first() }
    val secondJob = async { second() }

    select {
        firstJob.onAwait { first -> createResult(first, secondJob) }
        secondJob.onAwait { second -> createResult(second, firstJob).map { it.reversed() } }
    }
}
