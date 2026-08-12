package de.mineking.hexo.web.pages.sandbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardModifierKeys
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.utils.types.present
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
import de.mineking.hexo.web.layout.rememberAppLayout
import de.mineking.hexo.web.rememberHdsRepositories
import de.mineking.hexo.web.rememberPrevious
import de.mineking.hexo.web.rememberSoundPlayer
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
                initial.isBlank() -> Board.withTurnNumbers()
                else -> initial.parseRectilinearStateBKETurnNotation(focusWinningRows = false)
            }

            board to null
        } catch (e: HexoNotationException) {
            Board.withTurnNumbers() to e.message
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
    Toggle {
        override fun SandboxBoardViewManager.handle(
            coordinate: CellCoordinate,
            modifiers: BoardModifierKeys,
            currentCell: Cell?,
            board: Board,
        ) {
            if (currentCell?.turn != null) return
            updateCell(coordinate, CellOverride(
                owner = when (currentCell?.owner) {
                    null -> CellOwner.X
                    CellOwner.X -> CellOwner.O
                    CellOwner.O -> null
                }.present(),
            ))
        }
    },
    X {
        override fun SandboxBoardViewManager.handle(
            coordinate: CellCoordinate,
            modifiers: BoardModifierKeys,
            currentCell: Cell?,
            board: Board,
        ) {
            if (currentCell?.turn != null) return
            updateCell(coordinate, CellOverride(
                owner = when (currentCell?.owner) {
                    CellOwner.X -> null
                    else -> CellOwner.X
                }.present(),
            ))
        }
    },
    O {
        override fun SandboxBoardViewManager.handle(
            coordinate: CellCoordinate,
            modifiers: BoardModifierKeys,
            currentCell: Cell?,
            board: Board,
        ) {
            if (currentCell?.turn != null) return
            updateCell(coordinate, CellOverride(
                owner = when (currentCell?.owner) {
                    CellOwner.O -> null
                    else -> CellOwner.O
                }.present(),
            ))
        }
    },
    Turn {
        override fun SandboxBoardViewManager.handle(
            coordinate: CellCoordinate,
            modifiers: BoardModifierKeys,
            currentCell: Cell?,
            board: Board,
        ) {
            if (currentCell?.owner != null) return

            val (player, turn) = board.findNextTurn()
            updateCell(coordinate, CellOverride(
                owner = player.present(),
                turn = turn.present(),
            ))
        }
    },
    ;

    abstract fun SandboxBoardViewManager.handle(
        coordinate: CellCoordinate,
        modifiers: BoardModifierKeys,
        currentCell: Cell?,
        board: Board,
    )
}

@Composable
fun Sandbox(boardViewManager: SandboxBoardViewManager) {
    val appLayout = rememberAppLayout()
    DisposableEffect(appLayout) {
        val previousStyle = appLayout.pageStyle
        appLayout.pageStyle = PageStyle.Raw
        onDispose { appLayout.pageStyle = previousStyle }
    }

    val repositories = rememberHdsRepositories()

    var viewport by remember { mutableStateOf<BoardViewport?>(null) }
    var board by boardViewManager.board
    var boardUpdateCause by remember { mutableStateOf(BoardUpdateCause.VisualEditor) }

    val transformedBoard = remember(board) {
        board.copy().focusWinningRows()
    }

    val placementMode = remember {
        mutableStateOf(
            when {
                !board.isEmpty(includeHighlights = false) -> CellPlacementMode.Turn
                else -> CellPlacementMode.Toggle
            },
        )
    }

    SandboxSounds(board)

    Div({ classes("min-h-0", "min-w-0", "flex-1", "flex", "flex-col", "md:flex-row") }) {
        Div({ classes("min-h-0", "min-w-0", "flex-1", "flex", "p-3", "md:p-6") }) {
            SandboxBoardPane(
                board = transformedBoard,
                boardViewManager = boardViewManager,
                placementMode = placementMode.value,
                viewport = viewport,
                onViewportChange = { viewport = it },
                onBoardInteraction = { boardUpdateCause = BoardUpdateCause.VisualEditor },
            )
        }
        Sidebar(
            repositories = repositories,
            placementMode = placementMode,
            board = transformedBoard,
            boardUpdateCause = boardUpdateCause,
            onBoardChange = { cause, updated ->
                boardUpdateCause = cause
                board = updated
                if (cause == BoardUpdateCause.Import) {
                    viewport = null
                }
            },
        )
    }
}

@Composable
private fun SandboxBoardPane(
    board: Board,
    boardViewManager: SandboxBoardViewManager,
    placementMode: CellPlacementMode,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport?) -> Unit,
    onBoardInteraction: () -> Unit,
) {
    BoardPane(
        board = board,
        readOnly = false,
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

@Composable
fun SandboxSounds(board: Board) {
    val soundPlayer = rememberSoundPlayer()
    val moveCount = remember(board.cells) {
        board.cells.count { (_, cell) -> cell.owner != null }
    }

    val lastMoveCount = rememberPrevious(moveCount) ?: Int.MAX_VALUE

    LaunchedEffect(moveCount) {
        if (moveCount > lastMoveCount) {
            soundPlayer.play(SoundEffect.TilePlaced)
        }
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
