package de.mineking.hexo.web.session

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.sync.client.WatchParty
import de.mineking.hexo.sync.client.asBoard
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface BoardViewManager {
    val board: State<Board>
    val currentMove: MutableState<Int>

    fun apply(interaction: BoardInteraction.HighlightBoardInteraction)
    fun clearHighlights()
}

open class LocalBoardViewManager : BoardViewManager {
    override val board = mutableStateOf(Board())
    override val currentMove = mutableStateOf(Int.MAX_VALUE)

    override fun apply(interaction: BoardInteraction.HighlightBoardInteraction) {
        board.value = board.value.copy()
            .also { interaction.apply(it) }
    }

    override fun clearHighlights() {
        board.value = Board()
    }
}

@OptIn(DelicateCoroutinesApi::class)
class WatchPartyBoardViewManager(val watchParty: WatchParty) : LocalBoardViewManager() {
    override val currentMove = run {
        val original = super.currentMove
        object : MutableState<Int> by super.currentMove {
            override var value: Int
                get() = original.value
                set(value) {
                    original.value = value
                    GlobalScope.launch {
                        watchParty.adjustMoveCount(value)
                    }
                }
        }
    }

    init {
        GlobalScope.launch {
            watchParty.data.collect {
                board.value = it.asBoard()
                currentMove.value = it.move
            }
        }
    }

    override fun apply(interaction: BoardInteraction.HighlightBoardInteraction) {
        super.apply(interaction)
        GlobalScope.launch {
            when (interaction) {
                is BoardInteraction.HighlightCell -> watchParty.highlightCell(interaction.coordinate, interaction.highlight)
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

    override fun clearHighlights() {
        super.clearHighlights()
        GlobalScope.launch {
            watchParty.update(celHighlights = emptyMap(), lineHighlights = emptyList())
        }
    }
}
