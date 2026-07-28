package de.mineking.hexo.sync.server

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.plusAssign
import de.mineking.hexo.core.Omissible
import de.mineking.hexo.sync.common.WatchPartyCellRequest
import de.mineking.hexo.sync.common.WatchPartyClearHighlightsRequest
import de.mineking.hexo.sync.common.WatchPartyData
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.sync.common.WatchPartyLineHighlightRequest
import de.mineking.hexo.sync.common.WatchPartyMoveCountRequest
import de.mineking.hexo.sync.common.WatchPartyNavigateRequest
import de.mineking.hexo.sync.common.WatchPartyNavigateTarget
import de.mineking.hexo.sync.common.WatchPartyRequest
import de.mineking.hexo.sync.common.WatchPartyUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration

@JvmInline
internal value class WatchPartyConnectionId(val value: String)

internal class WatchPartyRequestException(override val message: String) : Exception(message)

internal class WatchPartySession private constructor(
    private val state: MutableStateFlow<WatchPartyState>,
    private val cleanupScope: CoroutineScope,
    private val connectionCleanupTimeout: Duration,
    private val lock: Mutex = Mutex(),
    private val removalJob: AtomicReference<Job?> = AtomicReference(null),
) {
    private data class ConnectionState(
        var count: Int = 0,
        var cleanupJob: Job? = null,
    )

    private val connectionLock = Any()
    private val connectionStates = mutableMapOf<WatchPartyConnectionId, ConnectionState>()
    private var removing = false

    val id get() = state.value.id

    constructor(
        id: WatchPartyId,
        cleanupScope: CoroutineScope,
        connectionCleanupTimeout: Duration,
    ) : this(
        state = MutableStateFlow(WatchPartyState(id = id, target = null)),
        cleanupScope = cleanupScope,
        connectionCleanupTimeout = connectionCleanupTimeout,
    )

    fun acquire(connectionId: WatchPartyConnectionId): Boolean {
        synchronized(connectionLock) {
            if (removing) return false

            connectionStates.getOrPut(connectionId) { ConnectionState() }.apply {
                count++
                cleanupJob?.cancel()
                cleanupJob = null
            }
        }
        removalJob.getAndSet(null)?.cancel()

        return true
    }

    private fun isUsed() = connectionStates.values.any { it.count > 0 }

    fun release(connectionId: WatchPartyConnectionId): Boolean {
        synchronized(connectionLock) {
            val connection = checkNotNull(connectionStates[connectionId])
            check(connection.count > 0)
            connection.count--

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

    private suspend fun removeHighlightsIfDisconnected(connectionId: WatchPartyConnectionId) {
        update { state ->
            synchronized(connectionLock) {
                if (connectionStates[connectionId]?.count != 0) return

                connectionStates.remove(connectionId)
                state.copy(target = state.target?.handleDisconnect(connectionId))
            }
        }
    }

    suspend inline fun update(block: (WatchPartyState) -> WatchPartyState) = lock.withLock {
        state.update(block)
    }

    suspend fun collect(
        connectionId: WatchPartyConnectionId,
        block: suspend (WatchPartyData) -> Unit,
    ): Nothing = state.collect { block(it.toDto(connectionId)) }

    suspend fun apply(request: WatchPartyRequest, connectionId: WatchPartyConnectionId): Unit = update {
        when (request) {
            is WatchPartyNavigateRequest -> applyNavigateRequest(it, request)
            is WatchPartyUpdateRequest -> applyUpdateRequest(it, request, connectionId)
            is WatchPartyMoveCountRequest -> applyMoveCountRequest(it, request)
            is WatchPartyCellRequest -> applyCellRequest(it, request, connectionId)
            is WatchPartyLineHighlightRequest -> applyLineHighlightRequest(it, request, connectionId)
            is WatchPartyClearHighlightsRequest -> applyClearHighlightsRequest(it, connectionId)
        }
    }

    private fun applyNavigateRequest(
        state: WatchPartyState,
        request: WatchPartyNavigateRequest,
    ): WatchPartyState {
        return state.copy(target = when (val target = request.target) {
            is WatchPartyNavigateTarget.Sandbox -> WatchPartyServerTarget.Sandbox(Board.withTurnNumbers())
            is WatchPartyNavigateTarget.Session -> WatchPartyServerTarget.Session(
                sessionId = target.id,
                move = Int.MAX_VALUE,
            )
            null -> null
        })
    }

    private fun applyUpdateRequest(
        state: WatchPartyState,
        request: WatchPartyUpdateRequest,
        connectionId: WatchPartyConnectionId,
    ): WatchPartyState {
        val target = state.target
            ?: throw WatchPartyRequestException("no watchparty target")

        return state.copy(target = when (target) {
            is WatchPartyServerTarget.Session -> target.copy(
                overlay = WatchPartySessionOverlay.fromBoard(request.board.removeOwners(), connectionId),
            )
            is WatchPartyServerTarget.Sandbox -> target.copy(board = request.board)
        })
    }

    private fun applyMoveCountRequest(
        state: WatchPartyState,
        request: WatchPartyMoveCountRequest,
    ): WatchPartyState {
        val target = state.target as? WatchPartyServerTarget.Session
            ?: throw WatchPartyRequestException("invalid watchparty target")

        return state.copy(target = target.copy(move = request.move))
    }

    private fun applyCellRequest(
        state: WatchPartyState,
        request: WatchPartyCellRequest,
        connectionId: WatchPartyConnectionId,
    ): WatchPartyState {
        val target = state.target
            ?: throw WatchPartyRequestException("no watchparty target")

        return state.copy(target = when (target) {
            is WatchPartyServerTarget.Session -> {
                val highlight = request.cell.highlight

                if (highlight is Omissible.Present) {
                    target.copy(
                        overlay = target.overlay.updateCell(
                            coordinate = request.coordinate,
                            highlight = highlight.value,
                            author = connectionId,
                        ),
                    )
                } else {
                    target
                }
            }
            is WatchPartyServerTarget.Sandbox -> target.copy(board = target.board.copy().apply {
                this@apply[request.coordinate] += request.cell.copy()
            })
        })
    }

    private fun applyLineHighlightRequest(
        state: WatchPartyState,
        request: WatchPartyLineHighlightRequest,
        connectionId: WatchPartyConnectionId,
    ): WatchPartyState {
        val target = state.target
            ?: throw WatchPartyRequestException("no watchparty target")

        fun Board.applyRequest() = copy().apply {
            if (request.remove) {
                lineHighlights -= request.line
            } else {
                lineHighlights += request.line
            }
        }

        return state.copy(target = when (target) {
            is WatchPartyServerTarget.Session -> {
                val overlay = if (request.remove) {
                    target.overlay.removeLine(request.line)
                } else {
                    target.overlay.addLine(request.line, connectionId)
                }

                target.copy(overlay = overlay)
            }
            is WatchPartyServerTarget.Sandbox -> target.copy(board = target.board.applyRequest())
        })
    }

    private fun applyClearHighlightsRequest(
        state: WatchPartyState,
        connectionId: WatchPartyConnectionId,
    ): WatchPartyState {
        val target = state.target
            ?: throw WatchPartyRequestException("no watchparty target")

        return state.copy(target = target.clearHighlightsBy(connectionId))
    }
}

private fun Board.removeOwners() = copy().apply {
    cells.forEach { (_, cell) ->
        cell.owner = null
    }
}
