package de.mineking.hexo.watchparty.client

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.utils.types.EntityId
import de.mineking.hexo.utils.types.present
import de.mineking.hexo.watchparty.protocol.WatchPartyCellRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyClearHighlightsRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import de.mineking.hexo.watchparty.protocol.WatchPartyLineHighlightRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyMoveCountRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyRedoRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyTargetDto
import de.mineking.hexo.watchparty.protocol.WatchPartyTransactionChildRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyTransactionRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyUndoRequest
import de.mineking.hexo.watchparty.protocol.WatchPartyUpdateRequest
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

sealed interface WatchPartyTarget {
    val generation: Long
    val hasClearableHighlights: Boolean
    suspend fun clearHighlights()

    suspend fun highlightCell(coordinate: CellCoordinate, highlight: CellHighlight?)
    suspend fun highlightLine(line: LineHighlight, remove: Boolean = false)

    interface Sandbox : WatchPartyTarget {
        suspend fun transaction(action: suspend () -> Unit)

        val board: Board
        suspend fun setBoard(board: Board)

        suspend fun updateCell(coordinate: CellCoordinate, override: CellOverride)

        fun canUndo(): Boolean
        suspend fun undo()

        fun canRedo(): Boolean
        suspend fun redo()
    }

    sealed interface AbstractGameWatchPartyTarget<T : EntityId> : WatchPartyTarget {
        val targetId: T

        val overlay: Board

        val currentMove: Int
        suspend fun setCurrentMove(move: Int)
    }

    interface FinishedGame : AbstractGameWatchPartyTarget<GameId>
    interface Session : AbstractGameWatchPartyTarget<SessionId>
}

internal sealed class AbstractWatchPartyTargetImpl(
    protected val watchparty: WatchParty,
    protected val data: WatchPartyDto,
) : WatchPartyTarget {
    override val generation = data.generation
    override val hasClearableHighlights = data.clearableHighlights

    protected open suspend fun request(action: WatchPartyRequest) {
        watchparty.request(action, generation)
    }

    override suspend fun clearHighlights() {
        request(WatchPartyClearHighlightsRequest)
    }

    override suspend fun highlightCell(coordinate: CellCoordinate, highlight: CellHighlight?) {
        request(WatchPartyCellRequest(coordinate, CellOverride(highlight = highlight.present())))
    }

    override suspend fun highlightLine(line: LineHighlight, remove: Boolean) {
        request(WatchPartyLineHighlightRequest(line, remove))
    }

    companion object {
        fun of(
            watchparty: WatchParty,
            data: WatchPartyDto,
        ) = when (val target = data.target) {
            is WatchPartyTargetDto.Sandbox -> SandboxWatchPartyTargetImpl(watchparty, data, target)
            is WatchPartyTargetDto.Game -> FinishedGameWatchPartyTargetImpl(watchparty, data, target)
            is WatchPartyTargetDto.Session -> SessionWatchPartyTargetImpl(watchparty, data, target)
            null -> null
        }
    }
}

internal class SandboxWatchPartyTargetImpl(
    watchparty: WatchParty,
    data: WatchPartyDto,
    private val target: WatchPartyTargetDto.Sandbox,
) : AbstractWatchPartyTargetImpl(watchparty, data), WatchPartyTarget.Sandbox {
    @Suppress("PropertyName")
    private val WatchPartyTransactionKey = object : CoroutineContext.Key<WatchPartyTransaction> {}

    private inner class WatchPartyTransaction : CoroutineContext.Element {
        override val key = WatchPartyTransactionKey
        private val lock = SynchronizedObject()

        private val _actions = mutableListOf<WatchPartyTransactionChildRequest>()
        val actions get() = synchronized(lock) { _actions.toList() }

        operator fun plusAssign(request: WatchPartyTransactionChildRequest) {
            synchronized(lock) {
                _actions += request
            }
        }
    }

    override suspend fun request(action: WatchPartyRequest) {
        require(action is WatchPartyTransactionChildRequest)

        val transaction = currentCoroutineContext()[WatchPartyTransactionKey]
        if (transaction == null) {
            super.request(action)
        } else {
            transaction += action
        }
    }

    override suspend fun transaction(action: suspend () -> Unit) {
        val (transaction, didCreate) = currentCoroutineContext()[WatchPartyTransactionKey]?.let { it to false }
            ?: (WatchPartyTransaction() to true)

        withContext(transaction) {
            action()
        }

        if (didCreate) {
            watchparty.request(WatchPartyTransactionRequest(transaction.actions), generation = generation)
        }
    }

    override val board = target.board

    override suspend fun setBoard(board: Board) {
        request(WatchPartyUpdateRequest(board))
    }

    override suspend fun updateCell(coordinate: CellCoordinate, override: CellOverride) {
        request(WatchPartyCellRequest(coordinate, override))
    }

    override fun canUndo() = target.canUndo
    override suspend fun undo() = request(WatchPartyUndoRequest)

    override fun canRedo() = target.canRedo
    override suspend fun redo() = request(WatchPartyRedoRequest)
}

internal class FinishedGameWatchPartyTargetImpl(
    watchparty: WatchParty,
    data: WatchPartyDto,
    target: WatchPartyTargetDto.Game,
) : AbstractWatchPartyTargetImpl(watchparty, data), WatchPartyTarget.FinishedGame {
    override val targetId = target.gameId
    override val overlay = target.overlay
    override val currentMove = target.move
    override suspend fun setCurrentMove(move: Int) {
        request(WatchPartyMoveCountRequest(move))
    }
}

internal class SessionWatchPartyTargetImpl(
    watchparty: WatchParty,
    data: WatchPartyDto,
    target: WatchPartyTargetDto.Session,
) : AbstractWatchPartyTargetImpl(watchparty, data), WatchPartyTarget.Session {
    override val targetId = target.sessionId
    override val overlay = target.overlay
    override val currentMove = target.move
    override suspend fun setCurrentMove(move: Int) {
        request(WatchPartyMoveCountRequest(move))
    }
}
