package de.mineking.hexo.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> completeImmediately(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> T,
): T {
    val deferred = CoroutineScope(context).async(start = CoroutineStart.UNDISPATCHED) {
        block()
    }

    if (!deferred.isCompleted) {
        deferred.cancel()
        error("Coroutine did not complete immediately")
    }

    return deferred.getCompleted()
}
