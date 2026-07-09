package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.board.render.compose.InteractiveBoard
import de.mineking.hexo.board.render.image.theme.HDSTheme
import de.mineking.hexo.web.components.LoadingIndicator
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement

val theme = HDSTheme.Default

@Composable
fun BoardPane(
    board: Board,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport) -> Unit,
    onBoardInteraction: (BoardInteraction) -> Unit,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: @Composable () -> Unit,
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
            attrs = {
                attr("width", "1200")
                attr("height", "900")
                classes("block", "h-full", "w-full", "touch-none")
                attrs?.invoke(this)
            },
        )

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

        content()
    }
}
