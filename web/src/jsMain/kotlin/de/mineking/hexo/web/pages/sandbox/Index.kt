package de.mineking.hexo.web.pages.sandbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.findNextTurn
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.board.render.compose.BoardModifierKeys
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.utils.types.present
import de.mineking.hexo.web.audio.SoundEffect
import de.mineking.hexo.web.board.AnalyzerTurn
import de.mineking.hexo.web.board.SandboxBoardViewManager
import de.mineking.hexo.web.board.rememberHostBoardViewManager
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.TextAreaInput
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.layout.PageStyle
import de.mineking.hexo.web.layout.rememberAppLayout
import de.mineking.hexo.web.rememberHdsRepositories
import de.mineking.hexo.web.rememberPrevious
import de.mineking.hexo.web.rememberQueryParameter
import de.mineking.hexo.web.rememberSoundPlayer
import org.jetbrains.compose.web.dom.Div

@InitRoute
fun initSandboxPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.Sandbox, style = PageStyle.Raw))
}

@Page
@Composable
fun SandboxPage() {
    var positionParameter by rememberQueryParameter("position")
    val (initialBoard, initialError) = remember {
        val initial = positionParameter?.replace("_", "/") ?: ""

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
    LaunchedEffect(Unit) { positionParameter = null }

    val boardViewManager = rememberHostBoardViewManager<SandboxBoardViewManager>()
    LaunchedEffect(initialBoard) {
        boardViewManager.board = initialBoard
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
        ) {
            if (currentCell?.owner != null) return

            if (board.attributes[BoardAttribute.ShowTurnNumbers] != true) {
                board = board.copy().apply {
                    cells.values.forEach { it.turn = null }
                    attributes[BoardAttribute.ShowTurnNumbers] = true
                }
            }

            val turn = board.findNextTurn()
            updateCell(coordinate, CellOverride(
                owner = turn.player.present(),
                turn = turn.turn.present(),
            ))
        }

        override fun analyzerTurn(board: Board): AnalyzerTurn {
            val turn = board.findNextTurn()
            return AnalyzerTurn(turn.player, turn.placementsRemaining)
        }
    },
    ;

    open fun analyzerTurn(board: Board): AnalyzerTurn? = null

    abstract fun SandboxBoardViewManager.handle(
        coordinate: CellCoordinate,
        modifiers: BoardModifierKeys,
        currentCell: Cell?,
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

    var viewport by remember { mutableStateOf(BoardViewport()) }

    val placementMode = remember {
        mutableStateOf(
            when {
                !boardViewManager.board.isEmpty(includeHighlights = false) -> CellPlacementMode.Turn
                else -> CellPlacementMode.Toggle
            },
        )
    }

    SandboxSounds(boardViewManager.board)

    Div({ classes("min-h-0", "min-w-0", "flex-1", "flex", "flex-col", "md:flex-row") }) {
        Div({ classes("min-h-0", "min-w-0", "flex-1", "flex", "p-3", "md:p-6") }) {
            SandboxBoardPane(
                boardViewManager = boardViewManager,
                placementMode = placementMode.value,
                viewport = viewport,
                onViewportChange = { viewport = it },
            )
        }
        if (!appLayout.fullscreen) {
            Sidebar(
                repositories = repositories,
                placementMode = placementMode,
                board = boardViewManager.board,
                onBoardChange = { boardViewManager.board = it },
                onImportPosition = { viewport = BoardViewport() },
            )
        }
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
