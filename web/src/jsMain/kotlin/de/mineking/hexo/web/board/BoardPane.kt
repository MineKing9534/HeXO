package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.GamePosition
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.compose.BoardContentBuilder
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardScope
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.board.render.compose.DEFAULT_CELL_HOVER_COlOR
import de.mineking.hexo.board.render.compose.InteractiveBoard
import de.mineking.hexo.board.render.image.BoardRenderingHook
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.center
import de.mineking.hexo.board.render.image.isCloseTo
import de.mineking.hexo.board.take
import de.mineking.hexo.board.toBoard
import de.mineking.hexo.game.model.game.GameMove
import de.mineking.hexo.game.model.game.GameWithPosition
import de.mineking.hexo.web.components.ActionButton
import de.mineking.hexo.web.components.ButtonSize
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.icons.ClearHighlightsIcon
import de.mineking.hexo.web.icons.EnterFullscreenIcon
import de.mineking.hexo.web.icons.ExitFullscreenIcon
import de.mineking.hexo.web.icons.ResetViewIcon
import de.mineking.hexo.web.layout.rememberAppLayout
import de.mineking.hexo.web.rememberTheme
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement

@Composable
fun GameWithPosition.rememberPosition(move: Int): GamePosition<GameMove> {
    return remember(this, move) { position.take(move) }
}

@Composable
fun BoardViewManager.transformBoard(key: Any, transform: (Board) -> Board): BoardViewManager {
    val board = remember(board, key) { transform(board) }
    return remember(board) {
        object : BoardViewManager by this {
            override val board = board
        }
    }
}

@Composable
fun BoardActionButton(
    enabled: Boolean = true,
    color: Color = Color.Neutral,
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) = ActionButton(
    enabled = enabled,
    size = ButtonSize.Medium,
    color = color,
    attrs = {
        classes("shadow-lg")
        attrs?.invoke(this)
    },
    onClick = onClick,
    content = content,
)

@Composable
fun BoardPane(
    boardViewManager: BoardViewManager,
    readOnly: Boolean,
    viewport: BoardViewport,
    onViewportChange: (BoardViewport) -> Unit,
    onBoardInteraction: (BoardInteraction) -> Unit,
    renderingHook: BoardRenderingHook? = null,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: BoardContentBuilder? = null,
) {
    if (AppGlobals.isExporting) {
        Div({
            classes(
                "grid", "grow", "place-items-center", "rounded-2xl", "border", "border-slate-800",
                "bg-linear-to-br", "from-slate-900", "to-slate-900/30", "shadow-2xl", "shadow-black/30", "h-full",
            )
        }) {
            LoadingIndicator { classes("size-9") }
        }
    }

    val readOnlyBoardHoverIndicator by SettingsKey.ReadOnlyBoardHoverIndicator.collectAsState()
    val theme by rememberTheme()

    Div({
        classes(
            "relative", "min-h-0", "min-w-0", "flex-1", "overflow-hidden", "rounded-2xl",
            "border", "border-slate-800", "bg-slate-900", "shadow-2xl",
        )
    }) {
        InteractiveBoard(
            board = boardViewManager.board,
            viewport = viewport,
            onViewportChange = onViewportChange,
            onBoardInteraction = onBoardInteraction,
            theme = theme,
            cellHoverColor = DEFAULT_CELL_HOVER_COlOR.takeIf { readOnlyBoardHoverIndicator || !readOnly },
            renderingHook = renderingHook,
            attrs = {
                attr("width", "1200")
                attr("height", "900")
                classes("block", "h-full", "w-full", "touch-none")
                attrs?.invoke(this)
            },
        ) {
            content?.invoke(this)
            DefaultBoardControls(boardViewManager, viewport, onViewportChange)
        }

        @Composable
        fun Edge(attrs: AttrBuilderContext<HTMLDivElement>? = null) {
            Div({
                style {
                    variable("--hexo-background", theme.backgroundColor.toString())
                }
                classes("pointer-events-none", "absolute", "z-10", "from-(--hexo-background)", "via-transparent", "to-transparent")
                attrs?.invoke(this)
            })
        }

        Edge { classes("inset-x-0", "top-0", "h-4", "bg-linear-to-b") }
        Edge { classes("inset-x-0", "bottom-0", "h-4", "bg-linear-to-t") }
        Edge { classes("inset-y-0", "left-0", "w-4", "bg-linear-to-r") }
        Edge { classes("inset-y-0", "right-0", "w-4", "bg-linear-to-l") }
    }
}

@Composable
private fun BoardScope.DefaultBoardControls(
    boardViewManager: BoardViewManager,
    viewport: BoardViewport,
    onViewportChange: (BoardViewport) -> Unit,
) {
    Div({ classes("absolute", "bottom-3", "right-3", "z-20", "flex", "gap-3") }) {
        if (boardViewManager.hasClearableHighlights) {
            BoardActionButton(onClick = { boardViewManager.clearHighlights() }, color = Color.Yellow) {
                ClearHighlightsIcon { classes("size-4") }
            }
        }

        BoardActionButton(onClick = {
            onViewportChange(nextHomeViewport(viewport))
        }) {
            ResetViewIcon { classes("size-4") }
        }

        FullScreenButton()
    }
}

@Composable
private fun FullScreenButton() {
    val layout = rememberAppLayout()

    DisposableEffect(Unit) {
        val previousSupportsFullScreen = layout.supportsFullScreen

        layout.supportsFullScreen = true
        onDispose { layout.supportsFullScreen = previousSupportsFullScreen }
    }

    BoardActionButton(onClick = { layout.fullscreen = !layout.fullscreen }) {
        if (layout.fullscreen) {
            ExitFullscreenIcon { classes("size-4") }
        } else {
            EnterFullscreenIcon { classes("size-4") }
        }
    }
}

private fun BoardScope.nextHomeViewport(viewport: BoardViewport) = BoardViewport(
    zoom = viewport.zoom,
    center = if (viewport.center.isCloseTo(Point.Zero)) {
        renderLayout.boundingBox.center
    } else {
        Point.Zero
    },
)
