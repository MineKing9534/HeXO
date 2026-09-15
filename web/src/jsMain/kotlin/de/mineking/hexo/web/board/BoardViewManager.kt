package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.hasHighlights
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.watchparty.client.WatchParty
import de.mineking.hexo.watchparty.client.WatchPartyTarget
import de.mineking.hexo.web.rememberWatchPartyController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

@Composable
fun rememberGameBoardViewManager(watchParty: WatchParty? = rememberWatchPartyController().currentWatchParty): GameBoardViewManager {
    val scope = rememberCoroutineScope()
    val manager = remember(watchParty) { GameBoardViewManagerImpl(watchParty, scope) }
    DisposableEffect(manager) { onDispose { manager.dispose() } }
    return manager
}

@Composable
fun rememberSandboxBoardViewManager(watchParty: WatchParty? = rememberWatchPartyController().currentWatchParty): SandboxBoardViewManager {
    val scope = rememberCoroutineScope()
    val manager = remember(watchParty) {
        if (watchParty == null) LocalSandboxBoardViewManager(scope) else WatchPartySandboxBoardViewManager(watchParty, scope)
    }
    DisposableEffect(manager) { onDispose { manager.dispose() } }
    return manager
}

interface BoardViewManager {
    val hasClearableHighlights: Boolean

    fun apply(interaction: BoardInteraction.HighlightBoardInteraction)
    fun clearHighlights()
}

interface GameBoardViewManager : BoardViewManager {
    val overlay: Board
    var currentMove: Int
}

interface SandboxBoardViewManager : BoardViewManager {
    var board: Board

    fun updateCell(coordinate: CellCoordinate, override: CellOverride)
    fun transaction(action: () -> Unit)

    fun canUndo(): Boolean
    fun canRedo(): Boolean

    fun undo()
    fun redo()
}

private abstract class AbstractBoardViewManager<T : WatchPartyTarget>(
    private val watchParty: WatchParty?,
    private val targetType: KClass<T>,
    parentScope: CoroutineScope,
) : BoardViewManager {
    protected val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
    private val edits = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    private var editingScope: Pair<CoroutineScope, T>? = null
    private var hasOptimisticUpdate = false

    override var hasClearableHighlights by mutableStateOf(false)

    @Suppress("UNCHECKED_CAST")
    protected val target: T?
        get() = watchParty?.target?.value
            ?.takeIf { targetType.isInstance(it) } as T?

    init {
        scope.launch {
            watchParty?.target?.collect {
                if (!hasOptimisticUpdate) {
                    restoreConfirmedState()
                }
            }
        }

        scope.launch {
            for (action in edits) {
                hasOptimisticUpdate = true
                try {
                    handleRequestFailure {
                        action()
                    }
                } finally {
                    hasOptimisticUpdate = false
                    restoreConfirmedState()
                }
            }
        }
    }

    protected abstract fun updateLocalBoard(transform: (Board) -> Board)
    protected abstract fun receiveTarget(target: T?)

    private fun restoreConfirmedState() {
        if (watchParty == null) return
        hasClearableHighlights = target?.hasClearableHighlights ?: false
        receiveTarget(target)
    }

    private fun enqueue(action: suspend T.() -> Unit) {
        val expected = target ?: return
        if (watchParty?.connected?.value != true) return

        edits.trySend {
            val current = target ?: return@trySend
            if (current.generation != expected.generation) return@trySend
            if (!watchParty.connected.value) return@trySend

            current.action()
        }
    }

    protected open fun updateState(action: () -> Unit) {
        if (watchParty == null || editingScope != null) return action()
        enqueue {
            if (this is WatchPartyTarget.Sandbox) {
                transaction {
                    withEditScope(this, action)
                }
            } else {
                withEditScope(this, action)
            }
        }
    }

    private suspend fun withEditScope(current: T, action: () -> Unit) = coroutineScope {
        check(editingScope == null)
        editingScope = this to current

        try {
            action()
        } finally {
            editingScope = null
        }
    }

    protected fun sendRequest(action: suspend T.() -> Unit) {
        val (scope, current) = editingScope ?: run {
            enqueue(action)
            return
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            current.action()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun handleRequestFailure(action: suspend () -> Unit) {
        try {
            action()
        } catch (e: CancellationException) {
            throw e
        } catch (error: Exception) {
            logger.warn(error) { "Watch party edit failed; restoring confirmed state" }
        }
    }

    override fun apply(interaction: BoardInteraction.HighlightBoardInteraction) = updateState {
        updateLocalBoard { board ->
            board.copy().also { interaction.apply(it) }
        }

        sendRequest {
            when (interaction) {
                is BoardInteraction.HighlightLine -> highlightLine(interaction.line, interaction.isRemove)
                is BoardInteraction.HighlightCell -> highlightCell(interaction.coordinate, interaction.highlight)
            }
        }
    }

    override fun clearHighlights() = updateState {
        // Game/session clearing is author-specific; only the server knows highlight ownership.
        if (target !is WatchPartyTarget.AbstractGameWatchPartyTarget<*>) {
            updateLocalBoard { board ->
                board.copy().apply {
                    lineHighlights.clear()
                    cells.values.forEach { it.highlight = null }
                }
            }
        }

        sendRequest { clearHighlights() }
    }

    fun dispose() {
        edits.cancel()
        scope.cancel()
    }
}

private abstract class AbstractSandboxBoardViewManager<T : WatchPartyTarget.Sandbox>(
    watchParty: WatchParty?,
    targetType: KClass<T>,
    parentScope: CoroutineScope,
) : AbstractBoardViewManager<T>(
    watchParty = watchParty,
    targetType = targetType,
    parentScope = parentScope,
), SandboxBoardViewManager {
    private var localBoard by mutableStateOf(Board())
    override var board: Board
        get() = localBoard
        set(value) = updateState {
            localBoard = value
            sendRequest { setBoard(value) }
        }

    override fun receiveTarget(target: T?) {
        updateLocalBoard { target?.board ?: Board() }
    }

    override fun updateLocalBoard(transform: (Board) -> Board) {
        localBoard = transform(board)
    }

    override fun updateCell(coordinate: CellCoordinate, override: CellOverride) = updateState {
        updateLocalBoard {
            it.copy().apply {
                this[coordinate] += override
            }
        }

        sendRequest { updateCell(coordinate, override) }
    }
}

private class WatchPartySandboxBoardViewManager(
    watchParty: WatchParty,
    parentScope: CoroutineScope,
) : AbstractSandboxBoardViewManager<WatchPartyTarget.Sandbox>(
    watchParty = watchParty,
    targetType = WatchPartyTarget.Sandbox::class,
    parentScope = parentScope,
) {
    private var undoAvailable by mutableStateOf(false)
    private var redoAvailable by mutableStateOf(false)

    override fun receiveTarget(target: WatchPartyTarget.Sandbox?) {
        super.receiveTarget(target)
        undoAvailable = target?.canUndo() ?: false
        redoAvailable = target?.canRedo() ?: false
    }

    override fun transaction(action: () -> Unit) = updateState(action)

    override fun canUndo() = undoAvailable
    override fun undo() = sendRequest { undo() }

    override fun canRedo() = redoAvailable
    override fun redo() = sendRequest { redo() }
}

private class LocalSandboxBoardViewManager(scope: CoroutineScope) : AbstractSandboxBoardViewManager<Nothing>(null, Nothing::class, scope) {
    override fun updateState(action: () -> Unit) = transaction(action)
    private val undoBoards = mutableStateListOf<Board>()
    private val redoBoards = mutableStateListOf<Board>()
    private var editing = false

    override fun transaction(action: () -> Unit) {
        if (editing) return action()

        val before = board.copy()
        editing = true

        @Suppress("TooGenericExceptionCaught")
        try {
            action()
            if (board != before) {
                undoBoards += before
                redoBoards.clear()
            }
        } catch (e: Throwable) {
            updateLocalBoard { before }
            throw e
        } finally {
            editing = false
            hasClearableHighlights = board.hasHighlights()
        }
    }

    override fun canUndo() = undoBoards.isNotEmpty()
    override fun canRedo() = redoBoards.isNotEmpty()

    override fun undo() {
        val previous = undoBoards.removeLastOrNull() ?: return
        redoBoards += board.copy()
        updateLocalBoard { previous.copy() }
        hasClearableHighlights = board.hasHighlights()
    }

    override fun redo() {
        val next = redoBoards.removeLastOrNull() ?: return
        undoBoards += board.copy()
        updateLocalBoard { next.copy() }
        hasClearableHighlights = board.hasHighlights()
    }
}

private class GameBoardViewManagerImpl(
    private val watchParty: WatchParty?,
    parentScope: CoroutineScope,
) : AbstractBoardViewManager<WatchPartyTarget.AbstractGameWatchPartyTarget<*>>(
    watchParty = watchParty,
    targetType = WatchPartyTarget.AbstractGameWatchPartyTarget::class,
    parentScope = parentScope,
), GameBoardViewManager {
    private var moveUpdateJob: Job? = null
    override var overlay by mutableStateOf(Board())
    private var move by mutableStateOf(Int.MAX_VALUE)
    override var currentMove: Int
        get() = move
        set(value) {
            move = value
            if (watchParty == null) return

            moveUpdateJob?.cancel()
            moveUpdateJob = scope.launch {
                delay(MOVE_UPDATE_DEBOUNCE)
                sendRequest { setCurrentMove(value) }
            }
        }

    override fun receiveTarget(target: WatchPartyTarget.AbstractGameWatchPartyTarget<*>?) {
        overlay = target?.overlay ?: Board()
        move = target?.currentMove ?: Int.MAX_VALUE
    }

    override fun updateLocalBoard(transform: (Board) -> Board) {
        overlay = transform(overlay)
        if (watchParty == null) hasClearableHighlights = overlay.hasHighlights()
    }

    private companion object {
        val MOVE_UPDATE_DEBOUNCE = 75.milliseconds
    }
}
