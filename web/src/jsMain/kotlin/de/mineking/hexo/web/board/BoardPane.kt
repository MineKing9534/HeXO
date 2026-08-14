package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.compose.BoardContentBuilder
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.board.render.compose.DEFAULT_CELL_HOVER_COlOR
import de.mineking.hexo.board.render.compose.InteractiveBoard
import de.mineking.hexo.board.render.image.BoardRenderingHook
import de.mineking.hexo.hds.model.AbstractGamePosition
import de.mineking.hexo.hds.model.asBoard
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.rememberTheme
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement

const val MOVES_PER_TURN = 2

@Composable
fun AbstractGamePosition.rememberBoard(overlay: Board, move: Int): Board {
    val board = remember(move, this) { asBoard(move) }
    return remember(board, overlay) {
        (board + overlay).focusWinningRows()
    }
}

@Composable
fun BoardPane(
    board: Board,
    readOnly: Boolean,
    viewport: BoardViewport?,
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
            board = board,
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
