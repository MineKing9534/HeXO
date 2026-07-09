package de.mineking.hexo.web.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.core.present
import de.mineking.hexo.web.audio.SoundEffect
import de.mineking.hexo.web.board.BoardPane
import de.mineking.hexo.web.board.SandboxBoardViewManager
import de.mineking.hexo.web.board.rememberHostBoardViewManager
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.ButtonSize
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.TextAreaInput
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.layout.PageStyle
import de.mineking.hexo.web.rememberHdsApiClient
import de.mineking.hexo.web.rememberPrevious
import de.mineking.hexo.web.rememberSoundPlayer
import de.mineking.hexo.web.sandbox.BoardUpdateCause
import de.mineking.hexo.web.sandbox.Sidebar
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Div

@InitRoute
fun initSandboxPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.Sandbox, style = PageStyle.Raw))
}

@Page
@Composable
fun SandboxPage(ctx: PageContext) {
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

    val boardViewManager = rememberHostBoardViewManager<SandboxBoardViewManager>()
    LaunchedEffect(initialBoard) {
        boardViewManager.board.value = initialBoard
    }

    Sandbox(boardViewManager)

    var error by remember { mutableStateOf(initialError) }
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
fun Sandbox(boardViewManager: SandboxBoardViewManager) {
    val client = rememberHdsApiClient()

    var viewport by remember { mutableStateOf<BoardViewport?>(null) }
    var board by boardViewManager.board

    val transformedBoard = remember(board) {
        board.copy().focusWinningRows()
    }

    val placementMode = remember {
        mutableStateOf(
            when {
                board.cells.values.any { it.owner != null } -> CellPlacementMode.Turn
                else -> CellPlacementMode.State
            },
        )
    }

    SandboxSounds(board)

    Div({ classes("min-h-0", "min-w-0", "flex-1", "flex", "flex-col", "md:flex-row") }) {
        Div({ classes("min-h-0", "min-w-0", "flex-1", "flex", "p-3", "md:p-6") }) {
            BoardPane(
                board = transformedBoard,
                readOnly = false,
                viewport = viewport,
                onViewportChange = { viewport = it },
                onBoardInteraction = { interaction ->
                    when (interaction) {
                        is BoardInteraction.PlaceCell -> boardViewManager.placeCell(interaction.coordinate, placementMode.value)
                        is BoardInteraction.HighlightBoardInteraction -> boardViewManager.apply(interaction)
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

@Composable
fun SandboxSounds(board: Board) {
    val soundPlayer = rememberSoundPlayer()
    val moveCount = remember(board.cells) {
        board.cells.count { (_, cell) -> cell.owner != null }
    }

    val lastMoveCount = rememberPrevious(moveCount) ?: 0

    LaunchedEffect(moveCount) {
        if (moveCount > lastMoveCount) {
            soundPlayer.play(SoundEffect.TilePlaced)
        }
    }
}

private fun Board.getMaxTurn() = cells.values.maxOfOrNull { it.turn ?: -1 }?.takeIf { it >= 0 }

private fun SandboxBoardViewManager.placeCell(coordinate: CellCoordinate, mode: CellPlacementMode) {
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

    when (mode) {
        CellPlacementMode.State if currentCell?.turn == null -> {
            updateCell(coordinate, CellOverride(
                owner = when (currentCell?.owner) {
                    null -> CellOwner.X
                    CellOwner.X -> CellOwner.O
                    CellOwner.O -> null
                }.present(),
            ))
        }

        CellPlacementMode.Turn -> {
            if (currentCell?.owner != null) return

            val (player, turn) = board.findNextTurn()
            updateCell(coordinate, CellOverride(
                owner = player.present(),
                turn = turn.present(),
            ))
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
