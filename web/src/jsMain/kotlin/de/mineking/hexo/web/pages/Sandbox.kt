package de.mineking.hexo.web.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import de.mineking.hexo.board.render.compose.InteractiveBoard
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.AppLayout
import de.mineking.hexo.web.components.AppPage
import de.mineking.hexo.web.components.ButtonSize
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.TextAreaInput
import de.mineking.hexo.web.rememberHdsApiClient
import de.mineking.hexo.web.sandbox.BoardUpdateCause
import de.mineking.hexo.web.sandbox.Sidebar
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

@Page
@Composable
fun SandboxPage(ctx: PageContext) {
    val client = rememberHdsApiClient(withSocket = false)
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

    AppLayout(activePage = AppPage.Sandbox, scrollContent = false, constrainContent = false) {
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
    val viewport = remember { mutableStateOf<BoardViewport?>(null) }
    val theme = remember { mutableStateOf(DefaultTheme.HDS) }
    val placementMode = remember {
        mutableStateOf(
            when {
                initialBoard.cells.values.any { it.owner != null } -> CellPlacementMode.Turn
                else -> CellPlacementMode.State
            },
        )
    }

    var board by remember { mutableStateOf(initialBoard) }
    val transformedBoard = remember(board) {
        board.copy().focusWinningRows()
    }

    Div({ classes("flex-1", "flex", "flex-col", "md:flex-row") }) {
        BoardPane(
            board = transformedBoard,
            theme = theme.value.theme,
            viewport = viewport,
            onBoardInteraction = { interaction ->
                board = board.copy().also {
                    when (interaction) {
                        is BoardInteraction.PlaceCell -> it.placeCell(interaction.coordinate, placementMode.value)
                        is BoardInteraction.HighlightBoardInteraction -> interaction.apply(it)
                    }
                }
            },
        )
        Sidebar(
            client = client,
            placementMode = placementMode,
            board = transformedBoard,
            onBoardChange = { cause, updated ->
                board = updated
                if (cause == BoardUpdateCause.Import) {
                    viewport.value = null
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

@Composable
private fun BoardPane(
    board: Board,
    theme: Theme,
    viewport: MutableState<BoardViewport?>,
    onBoardInteraction: (BoardInteraction) -> Unit,
) {
    var viewport by viewport
    Div({ classes("flex-1", "p-3", "md:p-6") }) {
        if (AppGlobals.isExporting) {
            Div({
                classes(
                    "grid", "grow", "place-items-center", "rounded-2xl", "border", "border-slate-800",
                    "bg-linear-to-br", "from-slate-900", "to-slate-900/30", "shadow-2xl", "shadow-black/30", "h-full",
                )
            }) {
                Div({ classes("size-9", "animate-spin", "rounded-full", "border-5", "border-slate-400/30", "border-t-emerald-400") })
            }
            return@Div
        }

        Div({ classes("relative", "h-full", "overflow-hidden", "rounded-2xl", "border", "border-slate-800", "bg-slate-900", "shadow-2xl") }) {
            InteractiveBoard(
                board = board,
                theme = theme,
                viewport = viewport,
                onViewportChange = { viewport = it },
                onBoardInteraction = onBoardInteraction,
                attrs = {
                    attr("width", "1200")
                    attr("height", "900")
                    classes("block", "h-full", "w-full", "touch-none")
                },
            )

            @Composable
            fun Edge(attrs: AttrBuilderContext<HTMLDivElement>? = null) {
                Div({
                    style {
                        variable("--hexo-background", theme.backgroundColor.toString())
                    }
                    classes("pointer-events-none", "absolute", "z-10", "from-(--hexo-background)", "to-transparent")
                    attrs?.invoke(this)
                })
            }

            Edge { classes("inset-x-0", "top-0", "h-4", "bg-linear-to-b") }
            Edge { classes("inset-x-0", "bottom-0", "h-4", "bg-linear-to-t") }
            Edge { classes("inset-y-0", "left-0", "w-4", "bg-linear-to-r") }
            Edge { classes("inset-y-0", "right-0", "w-4", "bg-linear-to-l") }

            ActionButton(
                label = "Reset View",
                size = ButtonSize.Medium,
                attrs = { classes("absolute", "bottom-3", "right-3", "z-20", "shadow-lg") },
                onClick = { viewport = null },
            )
        }
    }
}
