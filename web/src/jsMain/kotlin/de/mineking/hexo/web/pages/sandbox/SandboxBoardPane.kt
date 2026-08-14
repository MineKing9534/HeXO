package de.mineking.hexo.web.pages.sandbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardModifierKeys
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.utils.types.present
import de.mineking.hexo.web.board.AnalysedBoardPane
import de.mineking.hexo.web.board.GamePlayer
import de.mineking.hexo.web.board.SandboxBoardViewManager
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.ButtonSize
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState

private val sandboxPlayers = CellOwner.entries.associateWith { GamePlayer(it.symbol, it) }

@Composable
fun SandboxBoardPane(
    board: Board,
    boardViewManager: SandboxBoardViewManager,
    placementMode: CellPlacementMode,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport?) -> Unit,
    onBoardInteraction: () -> Unit,
) {
    val shouldAnalyze by SettingsKey.SandboxAnalyzer.collectAsState()

    AnalysedBoardPane(
        board = board,
        readOnly = false,
        allowAnalyzerOverlay = true,
        turn = if (shouldAnalyze) placementMode.analyzerTurn(board) else null,
        players = sandboxPlayers,
        viewport = viewport,
        onViewportChange = onViewportChange,
        onBoardInteraction = { interaction ->
            onBoardInteraction()
            when (interaction) {
                is BoardInteraction.PlaceCell -> boardViewManager.placeCell(
                    interaction.coordinate,
                    interaction.modifiers,
                    placementMode,
                )
                is BoardInteraction.HighlightBoardInteraction -> boardViewManager.apply(interaction)
            }
        },
    ) {
        ActionButton(
            label = "Reset View",
            size = ButtonSize.Medium,
            attrs = { classes("absolute", "bottom-3", "right-3", "z-20", "shadow-lg") },
            onClick = { onViewportChange(null) },
        )
    }
}

private fun Board.getMaxTurn() = cells.values.maxOfOrNull { it.turn ?: -1 }?.takeIf { it >= 0 }

private fun SandboxBoardViewManager.placeCell(coordinate: CellCoordinate, modifiers: BoardModifierKeys, mode: CellPlacementMode) {
    val board = board.value
    val maxTurn = board.getMaxTurn()

    val currentCell = board.cells[coordinate]
    if (currentCell?.turn != null && currentCell.turn == maxTurn) {
        updateCell(coordinate, CellOverride(
            owner = null.present(),
            turn = null.present(),
        ))
        return
    }

    mode.run {
        handle(coordinate, modifiers, currentCell, board)
    }
}
