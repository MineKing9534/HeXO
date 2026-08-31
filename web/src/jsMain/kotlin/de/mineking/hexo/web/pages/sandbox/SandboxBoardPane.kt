package de.mineking.hexo.web.pages.sandbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardModifierKeys
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.utils.types.present
import de.mineking.hexo.web.board.AnalysedBoardPane
import de.mineking.hexo.web.board.GamePlayer
import de.mineking.hexo.web.board.SandboxBoardViewManager
import de.mineking.hexo.web.board.transformBoard
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState

private val sandboxPlayers = CellOwner.entries.associateWith { GamePlayer(it.symbol, it) }

@Composable
fun SandboxBoardPane(
    boardViewManager: SandboxBoardViewManager,
    placementMode: CellPlacementMode,
    viewport: BoardViewport,
    onViewportChange: (BoardViewport) -> Unit,
) {
    val shouldAnalyze by SettingsKey.SandboxAnalyzer.collectAsState()

    AnalysedBoardPane(
        boardViewManager = boardViewManager.transformBoard(Unit) {
            it.copy().focusWinningRows()
        },
        readOnly = false,
        allowAnalyzerOverlay = true,
        turn = if (shouldAnalyze) placementMode.analyzerTurn(boardViewManager.board) else null,
        players = sandboxPlayers,
        viewport = viewport,
        onViewportChange = onViewportChange,
        onBoardInteraction = { interaction ->
            when (interaction) {
                is BoardInteraction.PlaceCell -> boardViewManager.placeCell(
                    interaction.coordinate,
                    interaction.modifiers,
                    placementMode,
                )
                is BoardInteraction.HighlightBoardInteraction -> boardViewManager.apply(interaction)
            }
        },
    )
}

private fun Board.getMaxTurn() = cells.values.maxOfOrNull { it.turn ?: -1 }?.takeIf { it >= 0 }

private fun SandboxBoardViewManager.placeCell(coordinate: CellCoordinate, modifiers: BoardModifierKeys, mode: CellPlacementMode) {
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
        if (currentCell?.turn == null) {
            run keyboard@{
                val new = when {
                    modifiers.ctrlKey -> CellOwner.X
                    modifiers.altKey || modifiers.shiftKey -> CellOwner.O
                    else -> return@keyboard
                }

                updateCell(coordinate, CellOverride(
                    owner = new.takeIf { it != currentCell?.owner }.present(),
                ))
                return@run
            }
        }

        handle(coordinate, modifiers, currentCell)
    }
}
