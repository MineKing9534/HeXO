package de.mineking.hexo.watchparty.server

import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.model.WatchPartyNavigateTarget
import de.mineking.hexo.watchparty.protocol.WatchPartyAction
import de.mineking.hexo.watchparty.protocol.WatchPartyCellRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyClearHighlightsRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import de.mineking.hexo.watchparty.protocol.WatchPartyLineHighlightRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyMoveCountRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyNavigateRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyRedoRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyTransactionRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyUndoRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration

@JvmInline
internal value class WatchPartyConnectionId(val value: String)

internal class WatchPartyRequestException(
    override val message: String,
    val state: WatchPartyDto? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

internal class WatchPartySession(
    val id: WatchPartyId,
    private val cleanupScope: CoroutineScope,
    private val connectionCleanupTimeout: Duration,
) {
    val state = WatchPartyState(id)
    private val removalJob: AtomicReference<Job?> = AtomicReference(null)

    private data class ConnectionState(
        var count: Int = 0,
        var cleanupJob: Job? = null,
        var detachGeneration: Long? = null,
    )

    private val connectionLock = Any()
    private val connectionStates = mutableMapOf<WatchPartyConnectionId, ConnectionState>()
    private var removing = false

    fun acquire(connectionId: WatchPartyConnectionId): Boolean {
        synchronized(connectionLock) {
            if (removing) return false

            connectionStates.getOrPut(connectionId) { ConnectionState() }.apply {
                count++
                cleanupJob?.cancel()
                cleanupJob = null
                detachGeneration = null
            }
        }
        removalJob.getAndSet(null)?.cancel()

        return true
    }

    private fun isUsed() = connectionStates.values.any { it.count > 0 }

    fun release(connectionId: WatchPartyConnectionId, detachOnClose: Boolean = false): Boolean {
        val generation = state.snapshot(connectionId).generation.takeIf { detachOnClose }

        synchronized(connectionLock) {
            val connection = checkNotNull(connectionStates[connectionId])
            check(connection.count > 0)
            connection.count--

            if (generation != null) connection.detachGeneration = generation

            if (connection.count == 0) scheduleConnectionCleanup(connectionId)
            return !isUsed()
        }
    }

    fun markRemovingIfUnused(): Boolean = synchronized(connectionLock) {
        if (isUsed()) return false

        removing = true
        true
    }

    fun scheduleRemoval(job: Job) {
        removalJob.getAndSet(job)?.cancel()
    }

    private fun scheduleConnectionCleanup(connectionId: WatchPartyConnectionId) {
        val connection = connectionStates[connectionId]
            ?.takeIf { it.count == 0 }
            ?: return

        connection.cleanupJob?.cancel()
        connection.cleanupJob = cleanupScope.launch {
            delay(connectionCleanupTimeout)
            removeHighlightsIfDisconnected(connectionId)
        }
    }

    private fun removeHighlightsIfDisconnected(connectionId: WatchPartyConnectionId) {
        val _ = state.update(connectionId) {
            synchronized(connectionLock) {
                val connection = connectionStates[connectionId] ?: return@update
                if (connection.count != 0) return@update

                connectionStates.remove(connectionId)
                if (connection.detachGeneration == generation) {
                    target = null
                } else {
                    target?.handleDisconnect()
                }
            }
        }
    }

    fun apply(
        action: WatchPartyAction,
        connectionId: WatchPartyConnectionId,
        expectedGeneration: Long? = null,
    ): Long = state.update(connectionId) {
        if (action is WatchPartyNavigateRequest) {
            if (!target.matches(action.target)) target = action.target.toServerTarget()
            return@update
        }

        if (expectedGeneration != null && expectedGeneration != generation) {
            throw WatchPartyRequestException("Watch party target changed")
        }

        val current = target ?: throw WatchPartyRequestException("no watchparty target")
        when (action) {
            is WatchPartyMoveCountRequest -> current.setMove(action.move)
            is WatchPartyUndoRequest -> current.undo()
            is WatchPartyRedoRequest -> current.redo()
            is WatchPartyTransactionRequest -> current.transaction(action.actions.map { it.toEdit() })
            else -> action.toEdit().apply(current)
        }
    }
}

private fun WatchPartyAction.toEdit(): WatchPartyEdit = when (this) {
    is WatchPartyUpdateRequest -> WatchPartyEdit.ReplaceBoard(board)
    is WatchPartyCellRequest -> WatchPartyEdit.UpdateCell(coordinate, cell)
    is WatchPartyLineHighlightRequest -> WatchPartyEdit.HighlightLine(line, remove)
    is WatchPartyClearHighlightsRequest -> WatchPartyEdit.ClearHighlights
    else -> throw WatchPartyRequestException("invalid sandbox transaction")
}

private fun WatchPartyNavigateTarget?.toServerTarget(): WatchPartyServerTarget? = when (this) {
    is WatchPartyNavigateTarget.Sandbox -> WatchPartyServerTarget.Sandbox()
    is WatchPartyNavigateTarget.Game -> WatchPartyServerTarget.Game(id)
    is WatchPartyNavigateTarget.Session -> WatchPartyServerTarget.Session(id)
    null -> null
}

private fun WatchPartyServerTarget?.matches(destination: WatchPartyNavigateTarget?): Boolean = when (destination) {
    is WatchPartyNavigateTarget.Sandbox -> this is WatchPartyServerTarget.Sandbox
    is WatchPartyNavigateTarget.Game -> this is WatchPartyServerTarget.Game && gameId == destination.id
    is WatchPartyNavigateTarget.Session -> this is WatchPartyServerTarget.Session && sessionId == destination.id
    null -> this == null
}
