package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.hasHighlights
import de.mineking.hexo.board.plusAssign
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.sync.client.WatchParty
import de.mineking.hexo.sync.common.WatchPartyTarget
import de.mineking.hexo.utils.types.present
import de.mineking.hexo.web.onSet
import de.mineking.hexo.web.rememberWatchPartyController
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
@Suppress("UNCHECKED_CAST")
fun rememberSubscriberBoardViewManager(watchParty: WatchParty): SubscriberBoardViewManager {
    return remember(watchParty) {
        WatchPartyBoardViewManager(watchParty)
    }
}

@Composable
fun <T : BoardViewManager> rememberHostBoardViewManager(): T {
    val watchPartyController = rememberWatchPartyController()

    @Suppress("UNCHECKED_CAST")
    return remember(watchPartyController.hostWatchParty) {
        watchPartyController.hostWatchParty
            ?.let { WatchPartyBoardViewManager(it) }
            ?: LocalBoardViewManager()
    } as T
}

interface BoardViewManager {
    val board: State<Board>
    val hasClearableHighlights: State<Boolean>

    fun apply(interaction: BoardInteraction.HighlightBoardInteraction)
    fun clearHighlights()
}

interface GameBoardViewManager : BoardViewManager {
    val currentMove: MutableState<Int>
}

interface SandboxBoardViewManager : BoardViewManager {
    override val board: MutableState<Board>

    fun updateCell(coordinate: CellCoordinate, override: CellOverride)
}

interface SubscriberBoardViewManager : GameBoardViewManager, SandboxBoardViewManager

private open class LocalBoardViewManager : SubscriberBoardViewManager {
    override val hasClearableHighlights = mutableStateOf(false)
    override val board = mutableStateOf(Board(), neverEqualPolicy()).onSet {
        updateHasClearableHighlights(it)
    }
    override val currentMove = mutableStateOf(Int.MAX_VALUE)

    protected open fun updateHasClearableHighlights(board: Board) {
        hasClearableHighlights.value = board.hasHighlights()
    }

    override fun apply(interaction: BoardInteraction.HighlightBoardInteraction) {
        board.value = board.value.copy()
            .also { interaction.apply(it) }
    }

    override fun updateCell(coordinate: CellCoordinate, override: CellOverride) {
        board.value = board.value.copy().apply {
            this[coordinate] += override
        }
    }

    override fun clearHighlights() {
        board.value = board.value.copy().apply {
            lineHighlights.clear()
            cells.values.forEach { it.highlight = null }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private class WatchPartyBoardViewManager(val watchParty: WatchParty) : LocalBoardViewManager() {
    private var suppressOutboundUpdate = false
    override val hasClearableHighlights = mutableStateOf(false)

    override val board = super.board.onSet {
        if (suppressOutboundUpdate) return@onSet

        GlobalScope.launch {
            watchParty.update(it)
        }
    }

    override fun updateHasClearableHighlights(board: Board) = Unit

    override val currentMove = super.currentMove.onSet {
        if (suppressOutboundUpdate) return@onSet

        GlobalScope.launch {
            watchParty.adjustMoveCount(it)
        }
    }

    private inline fun suppressOutboundUpdate(block: () -> Unit) {
        suppressOutboundUpdate = true
        try {
            block()
        } finally {
            suppressOutboundUpdate = false
        }
    }

    init {
        GlobalScope.launch {
            watchParty.data.collect {
                hasClearableHighlights.value = it.clearableHighlights

                suppressOutboundUpdate {
                    when (val target = it.target) {
                        is WatchPartyTarget.Sandbox -> {
                            board.value = target.board
                            currentMove.value = Int.MAX_VALUE
                        }
                        is WatchPartyTarget.Session -> {
                            board.value = target.overlay
                            currentMove.value = target.move
                        }
                        is WatchPartyTarget.Game -> {
                            board.value = target.overlay
                            currentMove.value = target.move
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun apply(interaction: BoardInteraction.HighlightBoardInteraction) {
        suppressOutboundUpdate {
            super.apply(interaction)
        }

        GlobalScope.launch {
            when (interaction) {
                is BoardInteraction.HighlightCell -> watchParty.updateCell(
                    coordinate = interaction.coordinate,
                    cell = CellOverride(highlight = interaction.highlight.present()),
                )
                is BoardInteraction.HighlightLine -> {
                    if (interaction.isRemove) {
                        watchParty.removeLine(interaction.line)
                    } else {
                        watchParty.addLine(interaction.line)
                    }
                }
            }
        }
    }

    override fun updateCell(coordinate: CellCoordinate, override: CellOverride) {
        suppressOutboundUpdate {
            super.updateCell(coordinate, override)
        }

        GlobalScope.launch {
            watchParty.updateCell(coordinate, override)
        }
    }

    override fun clearHighlights() {
        GlobalScope.launch {
            watchParty.clearHighlights()
        }
    }
}
