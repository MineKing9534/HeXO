package de.mineking.hexo.web.sandbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.parse.focusWinningRows
import de.mineking.hexo.board.render.notation.RectilinearNotationType
import de.mineking.hexo.board.render.notation.renderRectilinearNotation
import de.mineking.hexo.board.render.notation.renderRectilinearStateBKETurnNotation
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.Select
import de.mineking.hexo.web.pages.CellPlacementMode
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL

private const val DEFAULT_SIDEBAR_WIDTH = 380
private const val MIN_SIDEBAR_WIDTH = 330
private const val MAX_SIDEBAR_WIDTH = 560

private val boardParser = BoardParser.Default.focusWinningRows()

enum class BoardUpdateCause {
    NotationInput,
    Import,
    RemoveTurnData,
}

@Composable
fun Sidebar(
    client: HdsApiClient?,
    placementMode: MutableState<CellPlacementMode>,
    board: Board,
    onBoardChange: (BoardUpdateCause, Board) -> Unit,
) {
    var width by remember { mutableStateOf(DEFAULT_SIDEBAR_WIDTH) }
    var resizing by remember { mutableStateOf(false) }

    SidebarResizeEffect(
        resizing = resizing,
        onResize = { width = it },
        onResizeEnd = { resizing = false },
    )

    fun startSidebarResize(event: SyntheticMouseEvent) {
        event.preventDefault()
        resizing = true
    }

    Div({
        classes(
            "relative", "flex", "max-h-[30dvh]", "w-full", "shrink-0", "flex-col", "gap-5", "overflow-y-auto", "border-t", "border-slate-800",
            "bg-slate-900/80", "p-4", "shadow-2xl", "md:max-h-none", "md:w-(--sidebar-width)", "md:gap-8", "md:border-l", "md:border-t-0", "md:p-6",
        )
        attr("style", "--sidebar-width: ${width}px")
    }) {
        var parseError by remember { mutableStateOf<String?>(null) }
        var notation by remember { mutableStateOf("") }

        LaunchedEffect(board) {
            notation = board.renderRectilinearStateBKETurnNotation()
            parseError = null
        }

        SidebarResizeHandle(resizing, ::startSidebarResize)
        SidebarHeader(parseError)

        Div({ classes("flex", "flex-col", "gap-2") }) {
            val coroutineScope = rememberCoroutineScope()
            NotationField(
                client = client,
                notation = notation,
                parseError = parseError,
                onChange = { cause, value ->
                    notation = value
                    if (value.isBlank()) {
                        onBoardChange(cause, Board())
                        return@NotationField
                    }

                    coroutineScope.launch {
                        try {
                            onBoardChange(cause, boardParser.parse(value))
                            parseError = null
                        } catch (e: HexoNotationException) {
                            parseError = e.message
                        }
                    }
                },
            )
            SidebarNotationInfo(board, onBoardChange, parseError)
        }

        PlacementMode(placementMode)
    }
}

@Composable
private fun PlacementMode(placementMode: MutableState<CellPlacementMode>) {
    var placementMode by placementMode
    Div({ classes("space-y-2") }) {
        Div({ classes("text-sm", "font-semibold", "uppercase", "text-slate-400") }) {
            Text("Placement Mode")
        }

        Select(CellPlacementMode.entries, current = placementMode, onChange = { placementMode = it })
    }
}

@Composable
private fun SidebarResizeEffect(
    resizing: Boolean,
    onResize: (Int) -> Unit,
    onResizeEnd: () -> Unit,
) {
    DisposableEffect(resizing) {
        if (!resizing) return@DisposableEffect onDispose {}

        val mouseMove = EventListener { event ->
            event.preventDefault()
            val mouseEvent = event as MouseEvent
            onResize(
                (window.innerWidth - mouseEvent.clientX).coerceIn(
                    MIN_SIDEBAR_WIDTH,
                    MAX_SIDEBAR_WIDTH,
                ),
            )
        }
        val mouseUp = EventListener {
            onResizeEnd()
        }

        window.addEventListener("mousemove", mouseMove)
        window.addEventListener("mouseup", mouseUp)
        onDispose {
            window.removeEventListener("mousemove", mouseMove)
            window.removeEventListener("mouseup", mouseUp)
        }
    }
}

@Composable
private fun SidebarResizeHandle(
    resizing: Boolean,
    onResizeStart: (SyntheticMouseEvent) -> Unit,
) {
    Div({
        onMouseDown(onResizeStart)
        classes("group", "absolute", "-left-1", "top-0", "hidden", "h-full", "w-2", "cursor-col-resize", "place-items-center", "md:grid")
        if (resizing) {
            classes("bg-slate-700/30")
        } else {
            classes("hover:bg-slate-700/20")
        }
    }) {
        Div({
            classes("h-10", "w-1", "rounded-full", "bg-slate-600", "opacity-60", "transition", "group-hover:opacity-100")
        })
    }
}

@Composable
private fun SidebarHeader(parseError: String?) {
    Div({ classes("flex", "items-center", "justify-between") }) {
        Div({ classes("text-lg", "font-semibold") }) {
            Text("Board")
        }
        ParseStatus(parseError == null)
    }
}

@Composable
private fun ParseStatus(valid: Boolean) {
    Span({
        classes("rounded-full", "px-3", "py-1", "m-px", "text-xs", "font-medium", "ring-1")
        if (valid) {
            classes("bg-emerald-500/15", "ring-emerald-400/30", "text-emerald-400")
        } else {
            classes("bg-rose-500/15", "ring-rose-400/30", "text-rose-400")
        }
    }) {
        Text(if (valid) "Parsed" else "Invalid")
    }
}

@Composable
private fun NotationField(
    client: HdsApiClient?,
    notation: String,
    parseError: String?,
    onChange: (BoardUpdateCause, String) -> Unit,
) {
    Div({ classes("space-y-2") }) {
        Div({ classes("relative", "min-h-8", "overflow-hidden") }) {
            Div({ classes("pt-1.5", "text-sm", "font-semibold", "uppercase", "text-slate-400") }) {
                Text("Notation")
            }
            Div({
                classes(
                    "absolute", "right-0", "top-0", "z-10", "max-w-full",
                    "before:pointer-events-none", "before:absolute", "before:-left-5", "before:top-0", "before:h-full", "before:w-5",
                    "before:bg-linear-to-r", "before:from-transparent", "before:to-slate-900/95", "before:content-['']",
                )
            }) {
                NotationActions(client, notation, onChange)
            }
        }

        TextArea {
            value(notation)
            placeholder("Board notation")
            onInput { onChange(BoardUpdateCause.NotationInput, it.value) }
            classes(
                "min-h-32", "w-full", "resize-y", "rounded-lg", "border-3", "p-3", "text-sm", "text-slate-100", "placeholder-slate-500",
                "outline-none", "transition", "font-mono", "border-slate-700", "bg-slate-950", "focus:border-emerald-400", "focus:bg-slate-800",
            )
            if (parseError != null) classes("border-rose-400!")
        }
    }
}

@Composable
fun ActionButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button({
        if (!enabled) disabled()
        classes(
            "rounded-md", "border", "border-slate-700", "bg-slate-950", "px-2.5", "py-1", "text-nowrap",
            "text-xs", "font-medium", "text-slate-300", "disabled:text-slate-400", "transition",
            "not-disabled:hover:bg-slate-800", "not-disabled:hover:text-slate-100",
        )
        onClick { onClick() }
    }) {
        Text(label)
    }
}

@Composable
private fun NotationActions(
    client: HdsApiClient?,
    notation: String,
    onChange: (BoardUpdateCause, String) -> Unit,
) {
    var importDialogOpen by remember { mutableStateOf(false) }
    Div({ classes("flex", "max-w-full", "justify-end", "gap-2") }) {
        var link by remember { mutableStateOf<String?>(null) }
        ActionButton("Copy Link", enabled = notation.isNotBlank()) {
            val url = URL(window.location.href)
            url.searchParams.set("position", notation.replace("/", "_"))
            link = url.toString()
        }

        if (link != null) {
            Dialog(
                title = "Position Link",
                onClose = { link = null },
            ) {
                Input(InputType.Url) {
                    value(link ?: "")
                    classes(
                        "w-full", "resize-y", "rounded-lg", "border-3", "p-3", "text-sm", "text-slate-100",
                        "outline-none", "transition", "font-mono", "border-slate-700", "bg-slate-950",
                        "focus:border-emerald-400", "focus:bg-slate-800", "text-ellipsis",
                    )
                    readOnly()
                }
            }
        }

        if (client != null) {
            ActionButton("Import Position") { importDialogOpen = true }
        }
    }

    if (importDialogOpen && client != null) {
        ImportDialog(
            formationRepository = client.formationRepository,
            finishedGameRepository = client.finishedGameRepository,
            onClose = { importDialogOpen = false },
            onConfirm = {
                importDialogOpen = false
                onChange(BoardUpdateCause.Import, it.renderRectilinearNotation(RectilinearNotationType.Compact))
            },
        )
    }
}

@Composable
private fun SidebarNotationInfo(board: Board, onBoardChange: (BoardUpdateCause, Board) -> Unit, parseError: String?) {
    Div({ classes("flex", "justify-between", "w-full", "h-6") }) {
        Div({
            classes("min-h-5", "text-sm", "leading-relaxed")
            if (parseError == null) {
                classes("text-slate-500")
            } else {
                classes("text-rose-400")
            }
        }) {
            Text(parseError ?: (if (board.cells.size == 1) "1 cell" else "${board.cells.size} cells"))
        }

        if (parseError == null && board.cells.any { it.value.turn != null }) {
            ActionButton("Remove Turn Data") {
                onBoardChange(BoardUpdateCause.RemoveTurnData, board.copy().apply {
                    cells.values.forEach { it.turn = null }
                })
            }
        }
    }
}
