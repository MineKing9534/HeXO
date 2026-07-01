package de.mineking.hexo.web.session

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.sync.client.WatchParty
import de.mineking.hexo.sync.client.asBoard
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface HighlightManager {
    val board: State<Board>

    fun apply(interaction: BoardInteraction.HighlightBoardInteraction)
    fun clearHighlights()
}

open class LocalHighlightManager : HighlightManager {
    override val board = mutableStateOf(Board())

    override fun apply(interaction: BoardInteraction.HighlightBoardInteraction) {
        board.value = board.value.copy()
            .also { interaction.apply(it) }
    }

    override fun clearHighlights() {
        board.value = Board()
    }
}

@OptIn(DelicateCoroutinesApi::class)
class WatchPartyHighlightManager(val watchParty: WatchParty) : LocalHighlightManager() {
    init {
        GlobalScope.launch {
            watchParty.data
                .map { it.asBoard() }
                .collect { board.value = it }
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
