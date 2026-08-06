package de.mineking.hexo.utils.coroutines

import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

internal expect val DefaultCoroutineDispatcher: CoroutineDispatcher

fun createCoroutineScope(logger: KLogger, dispatcher: CoroutineDispatcher = DefaultCoroutineDispatcher): CoroutineScope {
    val parent = SupervisorJob()
    return CoroutineScope(dispatcher + parent + CoroutineExceptionHandler { _, throwable ->
        logger.error(throwable) { "Uncaught exception from coroutine" }
        if (throwable is Error) {
            parent.cancel()
            throw throwable
        }
    })
}
