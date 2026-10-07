package de.mineking.hexo.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

fun CoroutineScope.createSupervised(context: CoroutineContext) = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[Job]) + context)
