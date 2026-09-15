package de.mineking.hexo.hds.implementation.utils

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@IgnorableReturnValue
@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
inline fun <T> SynchronizedObject.withLock(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return synchronized(this) {
        block()
    }
}
