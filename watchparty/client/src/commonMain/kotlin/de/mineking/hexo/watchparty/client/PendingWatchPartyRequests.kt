package de.mineking.hexo.watchparty.client

import de.mineking.hexo.watchparty.protocol.WatchPartyRequestId
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred

class WatchPartyRequestException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal class PendingWatchPartyRequests(private var confirmedRevision: Long) {
    private class PendingRequest {
        val completion = CompletableDeferred<Unit>()
        var acceptedRevision: Long? = null
    }

    private val lock = SynchronizedObject()
    private val pending = mutableMapOf<WatchPartyRequestId, PendingRequest>()

    fun register(id: WatchPartyRequestId) = synchronized(lock) {
        PendingRequest().also { pending[id] = it }.completion
    }

    fun discard(id: WatchPartyRequestId) {
        remove(id)?.completion?.cancel()
    }

    private fun remove(id: WatchPartyRequestId) = synchronized(lock) { pending.remove(id) }

    fun accept(id: WatchPartyRequestId, revision: Long) {
        val completed = synchronized(lock) {
            val request = pending[id] ?: return
            request.acceptedRevision = revision
            if (revision <= confirmedRevision) pending.remove(id) else null
        }
        completed?.completion?.complete(Unit)
    }

    fun confirmState(revision: Long) {
        val completed = synchronized(lock) {
            confirmedRevision = maxOf(confirmedRevision, revision)
            val ready = pending.filterValues { it.acceptedRevision?.let { accepted -> accepted <= confirmedRevision } == true }
            ready.keys.forEach { pending.remove(it) }
            ready.values.toList()
        }
        completed.forEach { it.completion.complete(Unit) }
    }

    fun reject(id: WatchPartyRequestId, error: WatchPartyRequestException) {
        remove(id)?.completion?.completeExceptionally(error)
    }

    fun failAll(error: WatchPartyRequestException) {
        val requests = synchronized(lock) { pending.values.toList().also { pending.clear() } }
        requests.forEach { it.completion.completeExceptionally(error) }
    }
}
