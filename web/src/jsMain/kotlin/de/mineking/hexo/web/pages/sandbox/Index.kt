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
import de.mineking.hexo.board.binary.BoardBinary
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.findNextTurn
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.render.compose.BoardModifierKeys
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.utils.types.present
import de.mineking.hexo.watchparty.client.WatchPartyTarget
import de.mineking.hexo.web.audio.SoundEffect
import de.mineking.hexo.web.board.AnalyzerTurn
import de.mineking.hexo.web.board.SandboxBoardViewManager
import de.mineking.hexo.web.board.rememberSandboxBoardViewManager
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.ButtonSize
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.icons.AlertTriangleIcon
import de.mineking.hexo.web.icons.ChevronDownIcon
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.layout.PageStyle
import de.mineking.hexo.web.layout.rememberAppLayout
import de.mineking.hexo.web.rememberHdsRepositories
import de.mineking.hexo.web.rememberPrevious
import de.mineking.hexo.web.rememberQueryParameter
import de.mineking.hexo.web.rememberSoundPlayer
import de.mineking.hexo.web.rememberWatchPartyController
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@InitRoute
fun initSandboxPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.Sandbox, style = PageStyle.Raw))
}

@Page
@Composable
fun SandboxPage() {
    var positionParameter by rememberQueryParameter("position")
    val hasInitialPosition = remember { !positionParameter.isNullOrBlank() }

    var initialBoard by remember { mutableStateOf(Board.withTurnNumbers()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val encodedPosition = positionParameter ?: return@LaunchedEffect

        try {
            initialBoard = BoardBinary.decodeFromString(encodedPosition)
        } catch (e: IllegalArgumentException) {
            error = e.message?.takeIf { it.isNotBlank() } ?: "The position data could not be decoded."
        } finally {
            positionParameter = null
        }
    }

    if (positionParameter != null) return

    val boardViewManager = rememberSandboxBoardViewManager()
    val watchPartyController = rememberWatchPartyController()
    LaunchedEffect(boardViewManager, initialBoard) {
        watchPartyController.awaitReady()
        if (hasInitialPosition) {
            // Wait for RootLayout to update the watchparty target.
            watchPartyController.hostWatchParty?.target
                ?.first { it is WatchPartyTarget.Sandbox }
        }
        if (hasInitialPosition || watchPartyController.currentWatchParty == null) {
            boardViewManager.board = initialBoard
        }
    }

    Sandbox(boardViewManager)

    error?.let { message ->
        InvalidPositionDialog(message, onClose = { error = null })
    }
}

@Composable
private fun InvalidPositionDialog(message: String, onClose: () -> Unit) {
    Dialog(
        title = "Invalid position link",
        supportingText = "The shared position could not be loaded.",
        onClose = onClose,
        actionRow = {
            ActionButton(
                label = "Continue to sandbox",
                tooltip = "Close this message and continue using the sandbox",
                size = ButtonSize.Medium,
                color = Color.Neutral,
                onClick = onClose,
            )
        },
    ) {
        Div({ classes("space-y-4") }) {
            Div({
                classes(
                    "flex", "items-start", "gap-3", "rounded-xl", "border", "border-rose-400/25",
                    "bg-rose-400/8", "p-4",
                )
            }) {
                Div({
                    classes("grid", "size-9", "shrink-0", "place-items-center", "rounded-lg", "bg-rose-400/10", "text-rose-300")
                }) {
                    AlertTriangleIcon { classes("size-5") }
                }
                Div({ classes("min-w-0", "space-y-1") }) {
                    Div({ classes("text-sm", "font-semibold", "text-rose-200") }) {
                        Text("This link doesn't contain a valid position")
                    }
                    Div({ classes("text-sm", "leading-relaxed", "text-slate-300") }) {
                        Text("It may be incomplete or use an unsupported format. Try copying the full link, or ask the sender to share it again.")
                    }
                }
            }

            Div({ classes("text-sm", "leading-relaxed", "text-slate-400") }) {
                Text("You can still use the sandbox to build a position or import another link.")
            }

            InvalidPositionDetails(message)
        }
    }
}

@Composable
private fun InvalidPositionDetails(message: String) {
    var detailsExpanded by remember(message) { mutableStateOf(false) }

    Div {
        Button({
            attr("type", "button")
            classes(
                "inline-flex", "items-center", "gap-1.5", "cursor-pointer", "rounded", "py-1",
                "text-xs", "text-slate-500", "transition-colors", "hover:text-slate-300",
                "focus-visible:outline-2", "focus-visible:outline-offset-2", "focus-visible:outline-sky-400",
            )
            attr("aria-expanded", detailsExpanded.toString())
            attr("aria-controls", "sandbox-position-error-details")
            onClick { detailsExpanded = !detailsExpanded }
        }) {
            ChevronDownIcon {
                classes("size-3.5", "shrink-0", "transition-transform", "motion-reduce:transition-none")
                if (!detailsExpanded) classes("-rotate-90")
            }
            Text("Technical details")
        }
        Div({
            id("sandbox-position-error-details")
            if (!detailsExpanded) attr("hidden", "")
            classes(
                "mt-2", "max-h-40", "overflow-y-auto", "whitespace-pre-wrap", "break-words",
                "rounded-lg", "bg-slate-950/60", "p-3", "font-mono", "text-xs", "leading-relaxed", "text-slate-400",
            )
        }) {
            Text(message)
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
