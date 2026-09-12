package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.hasHighlights
import de.mineking.hexo.board.plusAssign
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.utils.types.present
import de.mineking.hexo.watchparty.client.WatchParty
import de.mineking.hexo.watchparty.common.WatchPartyTarget
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
    val board: Board
    val hasClearableHighlights: Boolean

    fun apply(interaction: BoardInteraction.HighlightBoardInteraction)
    fun clearHighlights()
}

interface GameBoardViewManager : BoardViewManager {
    var currentMove: Int
}

interface SandboxBoardViewManager : BoardViewManager {
    override var board: Board

    fun updateCell(coordinate: CellCoordinate, override: CellOverride)
}

interface SubscriberBoardViewManager : GameBoardViewManager, SandboxBoardViewManager

private open class LocalBoardViewManager : SubscriberBoardViewManager {
    override var hasClearableHighlights by mutableStateOf(false)
    override var board by mutableStateOf(Board(), neverEqualPolicy()).onSet {
        updateHasClearableHighlights(it)
    }
    override var currentMove by mutableStateOf(Int.MAX_VALUE)

    protected open fun updateHasClearableHighlights(board: Board) {
        hasClearableHighlights = board.hasHighlights()
    }

    override fun apply(interaction: BoardInteraction.HighlightBoardInteraction) {
        board = board.copy()
            .also { interaction.apply(it) }
    }

    override fun updateCell(coordinate: CellCoordinate, override: CellOverride) {
        board = board.copy().apply {
            this[coordinate] += override
        }
    }

    override fun clearHighlights() {
        board = board.copy().apply {
            lineHighlights.clear()
            cells.values.forEach { it.highlight = null }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private class WatchPartyBoardViewManager(val watchParty: WatchParty) : LocalBoardViewManager() {
    private var suppressOutboundUpdate = false
    override var hasClearableHighlights by mutableStateOf(false)

    override var board
        get() = super.board
        set(value) {
            super.board = value
            if (!suppressOutboundUpdate) {
                GlobalScope.launch {
                    watchParty.update(value)
                }
            }
        }

    override fun updateHasClearableHighlights(board: Board) = Unit

    override var currentMove
        get() = super.currentMove
        set(value) {
            super.currentMove = value
            if (!suppressOutboundUpdate) {
                GlobalScope.launch {
                    watchParty.adjustMoveCount(value)
                }
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
                hasClearableHighlights = it.clearableHighlights

                suppressOutboundUpdate {
                    when (val target = it.target) {
                        is WatchPartyTarget.Sandbox -> {
                            board = target.board
                            currentMove = Int.MAX_VALUE
                        }
                        is WatchPartyTarget.Session -> {
                            board = target.overlay
                            currentMove = target.move
                        }
                        is WatchPartyTarget.Game -> {
                            board = target.overlay
                            currentMove = target.move
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
