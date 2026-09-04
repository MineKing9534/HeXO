package de.mineking.hexo.web.pages.sandbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.css.borderColor
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.findNextTurn
import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.parse.focusWinningRows
import de.mineking.hexo.board.render.notation.RectilinearNotationType
import de.mineking.hexo.board.render.notation.renderRectilinearNotation
import de.mineking.hexo.board.render.notation.renderRectilinearStateBKETurnNotation
import de.mineking.hexo.game.model.RepositoryContainer
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.Checkbox
import de.mineking.hexo.web.components.CopyButton
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.ResizableTrailingPanel
import de.mineking.hexo.web.components.TextAreaInput
import de.mineking.hexo.web.components.TextInput
import de.mineking.hexo.web.icons.CloseIcon
import de.mineking.hexo.web.icons.CopyIcon
import de.mineking.hexo.web.icons.DownloadIcon
import de.mineking.hexo.web.icons.SandboxIcon
import de.mineking.hexo.web.playerCssColor
import de.mineking.hexo.web.rememberTheme
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.url.URL

private const val DEFAULT_SIDEBAR_WIDTH = 380
private const val MIN_SIDEBAR_WIDTH = 330
private const val MAX_SIDEBAR_WIDTH = 560

private val boardParser = BoardParser.Default.focusWinningRows()

@Composable
fun Sidebar(
    repositories: RepositoryContainer?,
    placementMode: MutableState<CellPlacementMode>,
    board: Board,
    onBoardChange: (Board) -> Unit,
    onImportPosition: () -> Unit,
) {
    ResizableTrailingPanel(
        defaultWidth = DEFAULT_SIDEBAR_WIDTH,
        minWidth = MIN_SIDEBAR_WIDTH,
        maxWidth = MAX_SIDEBAR_WIDTH,
        attrs = {
            classes(
                "relative", "flex", "min-h-0", "max-h-[38dvh]", "w-full", "shrink-0", "flex-col", "gap-3",
                "overflow-y-auto", "border-t", "border-slate-800", "bg-slate-900/90", "p-3", "shadow-2xl",
                "md:max-h-none", "md:w-(--sidebar-width)", "md:border-l", "md:border-t-0", "md:p-5",
            )
        },
    ) {
        var parseError by remember { mutableStateOf<String?>(null) }
        var notation by remember { mutableStateOf("") }

        LaunchedEffect(board) {
            val notationBoard = try {
                if (notation.isBlank()) Board.withTurnNumbers() else boardParser.parse(notation)
            } catch (_: HexoNotationException) {
                null
            }

            if (notationBoard != board) {
                notation = board.renderRectilinearStateBKETurnNotation()
            }
            parseError = null
        }

        SidebarHeader(parseError)
        SidebarNotationSection(
            repositories = repositories,
            board = board,
            notation = notation,
            parseError = parseError,
            onNotationChange = { notation = it },
            onParseErrorChange = { parseError = it },
            onBoardChange = onBoardChange,
            onImportPosition = onImportPosition,
        )

        SandboxTools(placementMode, board)
    }
}

@Composable
private fun SidebarNotationSection(
    repositories: RepositoryContainer?,
    board: Board,
    notation: String,
    parseError: String?,
    onNotationChange: (String) -> Unit,
    onParseErrorChange: (String?) -> Unit,
    onBoardChange: (Board) -> Unit,
    onImportPosition: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Div({
        classes(
            "flex", "flex-col", "gap-3", "rounded-2xl", "border", "border-slate-800",
            "bg-slate-800/40", "p-4", "shadow-xl", "shadow-black/10",
        )
    }) {
        SidebarSectionHeader(
            title = "Position notation",
            description = "Edit the board directly or import an existing position",
            accent = "bg-sky-400",
        )
        NotationField(notation, parseError) { value ->
            onNotationChange(value)
            if (value.isBlank()) {
                onBoardChange(Board.withTurnNumbers())
                return@NotationField
            }
            coroutineScope.launch {
                try {
                    onBoardChange(boardParser.parse(value))
                    onParseErrorChange(null)
                } catch (e: HexoNotationException) {
                    onParseErrorChange(e.message)
                }
            }
        }
        SidebarNotationInfo(board, onBoardChange, parseError) {
            NotationActions(repositories, notation) { value ->
                onNotationChange(value)
                onImportPosition()
            }
        }
    }
}

@Composable
private fun SandboxTools(placementMode: MutableState<CellPlacementMode>, board: Board) {
    Div({
        classes(
            "overflow-hidden", "rounded-2xl", "border", "border-slate-800", "bg-slate-800/40",
            "shadow-xl", "shadow-black/10",
        )
    }) {
        Div({ classes("space-y-3", "p-4") }) {
            SidebarSectionHeader(
                title = "Placement",
                description = "Choose how clicks modify cells",
                accent = "bg-emerald-400",
            )
            PlacementMode(placementMode, board)
        }
        Div({ classes("mx-4", "h-px", "bg-slate-700/50") })
        Div({ classes("space-y-3", "p-4") }) {
            SidebarSectionHeader(
                title = "Analysis",
                description = "Evaluate the current board while editing",
                accent = "bg-violet-400",
            )
            SandboxAnalyzerState(placementMode.value)
        }
    }
}

@Composable
private fun SandboxAnalyzerState(placementMode: CellPlacementMode) {
    var enabled by SettingsKey.SandboxAnalyzer.collectAsState()
    Div({
        classes(
            "flex", "items-center", "justify-between", "gap-4", "rounded-lg", "border",
            "border-slate-800", "bg-slate-950/30", "px-3", "py-2.5",
        )
    }) {
        Div({ classes("min-w-0") }) {
            Div({ classes("flex", "flex-wrap", "items-center", "gap-x-2", "gap-y-1") }) {
                Div({ classes("text-sm", "font-semibold", "text-slate-200") }) { Text("Sandbox analyzer") }
                if (placementMode != CellPlacementMode.Turn) {
                    Div({ classes("text-[10px]", "font-semibold", "uppercase", "tracking-wide", "text-amber-300/75") }) {
                        Text("Turn mode only")
                    }
                }
            }
            Div({ classes("mt-1", "text-xs", "leading-relaxed", "text-slate-500") }) {
                Text("Find forced wins with ")
                A(href = "https://github.com/SootyOwl/hexo-strix", {
                    target(ATarget.Blank)
                    classes("font-bold", "text-sky-400")
                }) {
                    Text("Strix")
                }
                Text(".")
            }
        }
        Checkbox(value = enabled, onValueChange = { enabled = it }) {
            classes("size-5", "shrink-0")
            attr("aria-label", "Sandbox analyzer")
        }
    }
}

@Composable
private fun PlacementMode(placementMode: MutableState<CellPlacementMode>, board: Board) {
    var placementMode by placementMode
    val theme by rememberTheme()
    Div({
        classes("grid", "grid-cols-2", "gap-2")
        attr("role", "group")
        attr("aria-label", "Placement mode")
    }) {
        CellPlacementMode.entries.forEach { mode ->
            val owners = mode.owners(board)
            Button({
                classes(
                    "flex", "min-w-0", "cursor-pointer", "items-center", "justify-between", "gap-2",
                    "rounded-lg", "border", "px-3", "py-2", "text-left", "transition-colors",
                    "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/40",
                )
                if (mode == placementMode) {
                    classes("border-slate-500", "bg-slate-800/70")
                    owners.singleOrNull()?.let { owner -> style { borderColor(theme.playerCssColor(owner)) } }
                    attr("aria-pressed", "true")
                } else {
                    classes(
                        "border-slate-700/70", "bg-slate-900/35", "hover:border-slate-600",
                        "hover:bg-slate-800/60",
                    )
                    attr("aria-pressed", "false")
                }
                onClick { placementMode = mode }
            }) {
                Div({ classes("min-w-0") }) {
                    Div({
                        classes(
                            "text-xs", "font-semibold",
                            if (mode == placementMode) "text-slate-100" else "text-slate-300",
                        )
                        if (mode == placementMode) {
                            owners.singleOrNull()?.let { owner -> style { color(theme.playerCssColor(owner)) } }
                        }
                    }) { Text(mode.toString()) }
                    Div({ classes("mt-0.5", "truncate", "text-[10px]", "text-slate-500") }) {
                        Text(mode.description)
                    }
                }
                Div({ classes("flex", "shrink-0", "gap-1") }) {
                    owners.forEach { owner ->
                        Div({
                            classes(
                                "size-2", "rounded-full",
                                if (mode == placementMode) "opacity-100" else "opacity-55",
                            )
                            style { backgroundColor(theme.playerCssColor(owner)) }
                        })
                    }
                }
            }
        }
    }
}

private val CellPlacementMode.description get() = when (this) {
    CellPlacementMode.Toggle -> "Cycle pieces"
    CellPlacementMode.X -> "Place X"
    CellPlacementMode.O -> "Place O"
    CellPlacementMode.Turn -> "Follow turns"
}

private fun CellPlacementMode.owners(board: Board) = when (this) {
    CellPlacementMode.Toggle -> CellOwner.entries
    CellPlacementMode.X -> listOf(CellOwner.X)
    CellPlacementMode.O -> listOf(CellOwner.O)
    CellPlacementMode.Turn -> listOf(board.findNextTurn().player)
}

@Composable
private fun SidebarHeader(parseError: String?) {
    Div({
        classes(
            "flex", "items-center", "justify-between", "gap-3", "px-1", "py-2",
        )
    }) {
        Div({ classes("flex", "min-w-0", "items-center", "gap-3") }) {
            Div({
                classes(
                    "grid", "size-9", "shrink-0", "place-items-center", "rounded-lg", "border",
                    "border-emerald-400/25", "bg-emerald-400/10", "text-emerald-300",
                )
            }) {
                SandboxIcon { classes("size-5") }
            }
            Div({ classes("min-w-0") }) {
                Div({ classes("text-lg", "font-bold", "leading-tight", "text-slate-100", "uppercase") }) {
                    Text("Sandbox")
                }
                Div({ classes("mt-0.5", "text-xs", "text-slate-500") }) {
                    Text("Build, inspect and share a board position")
                }
            }
        }
        ParseStatus(parseError == null)
    }
}

@Composable
private fun SidebarSectionHeader(title: String, description: String, accent: String) {
    Div({ classes("flex", "items-start", "gap-2.5") }) {
        Div({ classes("mt-1.5", "h-4", "w-1", "shrink-0", "rounded-full", accent) })
        Div({ classes("min-w-0") }) {
            Div({ classes("text-xs", "font-bold", "uppercase", "tracking-[0.14em]", "text-slate-200") }) {
                Text(title)
            }
            Div({ classes("mt-0.5", "text-xs", "leading-snug", "text-slate-500") }) {
                Text(description)
            }
        }
    }
}

@Composable
private fun ParseStatus(valid: Boolean) {
    Div({
        classes(
            "flex", "shrink-0", "items-center", "gap-1.5", "text-xs", "font-medium",
            if (valid) "text-slate-400" else "text-rose-300",
        )
    }) {
        Div({
            classes(
                "size-1.5", "rounded-full",
                if (valid) "bg-emerald-400" else "bg-rose-400",
            )
        })
        Text(if (valid) "Valid" else "Invalid")
    }
}

@Composable
private fun NotationField(
    notation: String,
    parseError: String?,
    onChange: (String) -> Unit,
) {
    Div {
        TextAreaInput(
            value = notation,
            placeholder = "Board notation",
            valid = parseError == null,
            monospace = true,
            attrs = {
                classes("min-h-36", "resize-y", "bg-slate-950/55!")
                if (parseError != null) classes("border-rose-400!")
            },
            onValueChange = onChange,
        )
    }
}

@Composable
private fun NotationActions(
    repositories: RepositoryContainer?,
    notation: String,
    onChange: (String) -> Unit,
) {
    var importDialogOpen by remember { mutableStateOf(false) }
    Div({ classes("flex", "shrink-0", "gap-1.5") }) {
        var link by remember { mutableStateOf<String?>(null) }
        ActionButton(
            enabled = notation.isNotBlank(),
            attrs = {
                classes("grid", "size-7!", "place-items-center", "p-0!")
                attr("aria-label", "Copy a link to this position")
                attr("title", "Copy a link to this position")
            },
            onClick = {
                val url = URL(window.location.href)
                url.searchParams.set("position", notation.replace("/", "_"))
                link = url.toString()
            },
        ) {
            CopyIcon { classes("size-3.5") }
        }

        if (link != null) PositionLinkDialog(link ?: "") { link = null }

        if (repositories != null) {
            ActionButton(
                attrs = {
                    classes("grid", "size-7!", "place-items-center", "p-0!")
                    attr("aria-label", "Import an existing position")
                    attr("title", "Import an existing position")
                },
                onClick = { importDialogOpen = true },
            ) {
                DownloadIcon { classes("size-3.5") }
            }
        }
    }

    if (importDialogOpen && repositories != null) {
        ImportDialog(
            formationRepository = repositories.formationRepository,
            finishedGameRepository = repositories.finishedGameRepository,
            onClose = { importDialogOpen = false },
            onConfirm = {
                importDialogOpen = false
                onChange(it.renderRectilinearNotation(RectilinearNotationType.Compact))
            },
        )
    }
}

@Composable
private fun PositionLinkDialog(link: String, onClose: () -> Unit) {
    Dialog(title = "Position Link", onClose = onClose) {
        Div({ classes("relative", "w-full") }) {
            TextInput(
                value = link,
                type = InputType.Url,
                readOnly = true,
                monospace = true,
                attrs = { classes("pr-12", "resize-y", "text-ellipsis") },
            )
            CopyButton(link, label = "position link")
        }
    }
}

@Composable
private fun SidebarNotationInfo(
    board: Board,
    onBoardChange: (Board) -> Unit,
    parseError: String?,
    actions: @Composable () -> Unit,
) {
    Div({ classes("flex", "w-full", "flex-wrap", "items-center", "justify-between", "gap-2") }) {
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

        Div({ classes("flex", "items-center", "gap-1.5") }) {
            if (parseError == null && board.cells.any { it.value.turn != null }) {
                ActionButton(
                    attrs = {
                        classes("grid", "size-7!", "place-items-center", "p-0!")
                        attr("aria-label", "Remove turn data")
                        attr("title", "Remove turn data")
                    },
                    onClick = {
                        onBoardChange(board.copy().apply {
                            cells.values.forEach { it.turn = null }
                        })
                    },
                ) {
                    CloseIcon { classes("size-4") }
                }
            }
            actions()
        }
    }
}
