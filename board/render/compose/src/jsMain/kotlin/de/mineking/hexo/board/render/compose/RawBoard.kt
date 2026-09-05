package de.mineking.hexo.board.render.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.render.image.BoardRenderBounds
import de.mineking.hexo.board.render.image.BoardRenderLayout
import de.mineking.hexo.board.render.image.BoardRenderingHook
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.createRenderLayout
import de.mineking.hexo.board.render.image.drawBoard
import de.mineking.hexo.board.render.image.plus
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.render.image.theme.withAlpha
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.ContentBuilder
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement

val DEFAULT_CELL_HOVER_COlOR = Color.rgb(0x7dd3fc)

typealias BoardContentBuilder = @Composable BoardScope.() -> Unit

class BoardScope(
    val renderLayout: BoardRenderLayout,
    val element: HTMLCanvasElement,
)

@Composable
fun RawBoard(
    board: Board,
    viewport: BoardViewport,
    onViewportChange: (BoardViewport) -> Unit,
    theme: Theme = Theme.Default,
    cellHoverColor: Color? = DEFAULT_CELL_HOVER_COlOR,
    renderingHook: BoardRenderingHook? = null,
    onCellClick: ((BoardLeftClickEvent) -> Unit)? = null,
    onBoardRightClick: ((BoardRightClickEvent) -> Unit)? = null,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    fallback: ContentBuilder<HTMLCanvasElement>? = null,
    content: BoardContentBuilder? = null,
) {
    var element by remember { mutableStateOf<HTMLCanvasElement?>(null) }
    var dragging by remember { mutableStateOf(false) }
    var hoveredCell by remember { mutableStateOf<CellCoordinate?>(null) }

    val layout = remember(board) {
        board.createRenderLayout(
            layoutRadius = 128.0,
            bounds = BoardRenderBounds.IncludeSurroundings,
            visibleRadius = DEFAULT_VISIBLE_RADIUS,
        )
    }

    val renderingHook by rememberUpdatedState(renderingHook)

    fun redraw() {
        element?.drawBoard(
            layout = layout,
            viewport = viewport,
            hoveredCell = hoveredCell,
            theme = theme,
            cellHoverColor = cellHoverColor,
            renderingHook = renderingHook,
        )
    }

    ResizeHandler(element) { redraw() }
    LaunchedEffect(viewport, layout, hoveredCell, theme, cellHoverColor, renderingHook, element) { redraw() }

    BoardInteractions(
        element = element,
        renderLayout = { layout },
        viewport = { viewport },
        onViewportChange = onViewportChange,
        onDraggingChange = { dragging = it },
        onCellHoverChange = { hoveredCell = it },
        onCellClick = { onCellClick?.invoke(it) },
        onBoardRightClick = { onBoardRightClick?.invoke(it) },
    )

    Canvas({
        if (attrs != null) attrs()
        style {
            cursor(if (dragging) "grabbing" else "grab")
        }
        ref {
            element = it
            onDispose {}
        }
    }, fallback)

    val scope = remember(layout, element) {
        element?.let { BoardScope(layout, it) }
    } ?: return
    content?.invoke(scope)
}

@Composable
private fun ResizeHandler(element: HTMLCanvasElement?, onResize: () -> Unit) {
    val onResize by rememberUpdatedState(onResize)

    DisposableEffect(element) {
        val canvas = element ?: return@DisposableEffect onDispose {}
        val observer = ResizeObserver { onResize() }

        observer.observe(canvas)
        onDispose {
            observer.disconnect()
        }
    }
}

private external class ResizeObserver(@Suppress("unused") callback: () -> Unit) {
    fun observe(target: Element)
    fun disconnect()
}

private fun HTMLCanvasElement.drawBoard(
    layout: BoardRenderLayout,
    viewport: BoardViewport,
    hoveredCell: CellCoordinate?,
    theme: Theme,
    cellHoverColor: Color?,
    renderingHook: BoardRenderingHook?,
) {
    if (width != clientWidth) width = clientWidth
    if (height != clientHeight) height = clientHeight

    drawBoard(
        layout = layout,
        padding = BOARD_RENDER_PADDING,
        offset = viewport.offset(this),
        scale = viewport.zoom,
        theme = theme,
        renderingHook = renderingHook + BoardRenderingHook.middleLayer {
            if (hoveredCell == null || cellHoverColor == null) return@middleLayer
            val cell = layout.board.cells[hoveredCell]

            theme.cellShape.run {
                backend.drawCellShape(
                    point = hoveredCell.toPixel(),
                    radius = hexSize,
                    color = cellHoverColor.withAlpha(48),
                    outline = if (cell != null && (cell.highlight != null || cell.focused)) null else Stroke(cellHoverColor.withAlpha(140), 2f),
                )
            }
        },
    )
}
