package de.mineking.hexo.web.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.AppLayout
import de.mineking.hexo.web.components.AppPage
import de.mineking.hexo.web.components.BoardPane
import de.mineking.hexo.web.components.ButtonSize
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.TextAreaInput
import de.mineking.hexo.web.rememberHdsApiClient
import de.mineking.hexo.web.sandbox.BoardUpdateCause
import de.mineking.hexo.web.sandbox.Sidebar
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Div

@Page
@Composable
fun SandboxPage(ctx: PageContext) {
    val client = rememberHdsApiClient()
    val (initialBoard, initialError) = remember {
        val initial = ctx.route.queryParams["position"]?.replace("_", "/") ?: ""
        if (!AppGlobals.isExporting) window.history.replaceState(null, "", window.location.pathname)

        try {
            val board = when {
                initial.isBlank() -> Board()
                else -> initial.parseRectilinearStateBKETurnNotation(focusWinningRows = false)
            }

            board to null
        } catch (e: HexoNotationException) {
            Board() to e.message
        }
    }

    var error by remember { mutableStateOf(initialError) }

    AppLayout(activePage = AppPage.Sandbox, padding = false) {
        MainLayout(client, initialBoard)
    }

    if (error != null) {
        Dialog(title = "Invalid Position", onClose = { error = null }) {
            TextAreaInput(
                value = error ?: "",
                valid = false,
                readOnly = true,
                monospace = true,
                attrs = { classes("resize-y", "border-rose-400", "text-rose-100") },
            )
        }
    }
}

enum class CellPlacementMode {
    State,
    Turn,
}

@Composable
private fun MainLayout(client: HdsApiClient?, initialBoard: Board) {
    val placementMode = remember {
        mutableStateOf(
            when {
                initialBoard.cells.values.any { it.owner != null } -> CellPlacementMode.Turn
                else -> CellPlacementMode.State
            },
        )
    }

    var board by remember { mutableStateOf(initialBoard) }
    var viewport by remember { mutableStateOf<BoardViewport?>(null) }
    val transformedBoard = remember(board) {
        board.copy().focusWinningRows()
    }

    Div({ classes("min-h-0", "min-w-0", "flex-1", "flex", "flex-col", "md:flex-row") }) {
        Div({ classes("min-h-0", "min-w-0", "flex-1", "flex", "p-3", "md:p-6") }) {
            BoardPane(
                board = transformedBoard,
                viewport = viewport,
                onViewportChange = { viewport = it },
                onBoardInteraction = { interaction ->
                    board = board.copy().also {
                        when (interaction) {
                            is BoardInteraction.PlaceCell -> it.placeCell(interaction.coordinate, placementMode.value)
                            is BoardInteraction.HighlightBoardInteraction -> interaction.apply(it)
                        }
                    }
                },
            ) {
                ActionButton(
                    label = "Reset View",
                    size = ButtonSize.Medium,
                    attrs = { classes("absolute", "bottom-3", "right-3", "z-20", "shadow-lg") },
                    onClick = { viewport = null },
                )
            }
        }
        Sidebar(
            client = client,
            placementMode = placementMode,
            board = transformedBoard,
            onBoardChange = { cause, updated ->
                board = updated
                if (cause == BoardUpdateCause.Import) {
                    viewport = null
                }
            },
        )
    }
}

private fun Board.getMaxTurn() = cells.values.maxOfOrNull { it.turn ?: -1 }?.takeIf { it >= 0 }

private fun MutableBoard.placeCell(coordinate: CellCoordinate, mode: CellPlacementMode) {
    val maxTurn = getMaxTurn()

    val currentCell = cells[coordinate]
    if (currentCell?.turn != null && currentCell.turn == maxTurn) {
        this[coordinate].owner = null
        this[coordinate].turn = null
        return
    }

    when (mode) {
        CellPlacementMode.State if currentCell?.turn == null -> {
            this[coordinate].owner = when (currentCell?.owner) {
                null -> CellOwner.X
                CellOwner.X -> CellOwner.O
                CellOwner.O -> null
            }
        }

        CellPlacementMode.Turn -> {
            if (currentCell?.owner != null) return

            val (player, turn) = findNextTurn()
            this[coordinate].apply {
                this.owner = player
                this.turn = turn
            }
        }

        else -> {}
    }
}

private fun Board.findNextTurn(): Pair<CellOwner, Int> {
    var hadPosition = false
    var turn = 0
    var isComplete = false
    var player = CellOwner.X

    cells.values.forEach { cell ->
        val cellOwner = cell.owner ?: return@forEach
        val cellTurn = cell.turn?.takeIf { it >= turn } ?: run {
            hadPosition = true
            return@forEach
        }

        if (cellTurn == turn) {
            isComplete = true
        } else {
            turn = cellTurn
            isComplete = false
            player = cellOwner
        }
    }

    if (turn == 0 && hadPosition) {
        turn = 1
        player = CellOwner.X
    } else if (isComplete) {
        turn++
        player = player.other
    }

    return player to turn
}
