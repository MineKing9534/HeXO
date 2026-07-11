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
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.Stroke
import de.mineking.hexo.board.render.image.center
import de.mineking.hexo.board.render.image.createHex
import de.mineking.hexo.board.render.image.createRenderLayout
import de.mineking.hexo.board.render.image.div
import de.mineking.hexo.board.render.image.drawBoard
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.render.image.theme.withAlpha
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.ContentBuilder
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement

private const val BOARD_LAYOUT_RADIUS = 255.0
val DEFAULT_CELL_HOVER_COlOR = Color.rgb(0x7dd3fc)

typealias BoardContentBuilder = @Composable BoardScope.() -> Unit
class BoardScope(
    val renderLayout: BoardRenderLayout,
    val element: HTMLCanvasElement,
)

@Composable
fun RawBoard(
    board: Board,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport) -> Unit,
    theme: Theme = Theme.Default,
    cellHoverColor: Color? = DEFAULT_CELL_HOVER_COlOR,
    middleLayer: RenderingContext.() -> Unit = {},
    onCellClick: ((CellCoordinate) -> Unit)? = null,
    onBoardRightClick: ((BoardRightClickEvent) -> Unit)? = null,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    fallback: ContentBuilder<HTMLCanvasElement>? = null,
    content: BoardContentBuilder? = null,
) {
    var element by remember { mutableStateOf<HTMLCanvasElement?>(null) }
    var dragging by remember { mutableStateOf(false) }
    var hoveredCell by remember { mutableStateOf<CellCoordinate?>(null) }

    val zoom = viewport?.zoom ?: 0.2
    val layout = remember(board, zoom) {
        board.createRenderLayout(
            layoutRadius = BOARD_LAYOUT_RADIUS * zoom,
            bounds = BoardRenderBounds.IncludeSurroundings,
            visibleRadius = DEFAULT_VISIBLE_RADIUS,
        )
    }
    val effectiveViewport = viewport ?: BoardViewport(zoom = zoom, center = layout.boundingBox.center / zoom).also { onViewportChange(it) }

    val middleLayer by rememberUpdatedState(middleLayer)

    fun redraw() {
        element?.drawBoard(
            layout = layout,
            viewport = effectiveViewport,
            hoveredCell = hoveredCell,
            theme = theme,
            cellHoverColor = cellHoverColor,
            middleLayer = middleLayer,
        )
    }

    ResizeHandler(element) { redraw() }
    LaunchedEffect(effectiveViewport, layout, hoveredCell, theme) { redraw() }

    BoardInteractions(
        element = element,
        renderLayout = { layout },
        viewport = { effectiveViewport },
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
    middleLayer: RenderingContext.() -> Unit = {},
) {
    width = clientWidth
    height = clientHeight

    drawBoard(
        layout = layout,
        padding = BOARD_RENDER_PADDING,
        offset = viewport.offset(this),
        theme = theme,
    ) {
        middleLayer()

        if (hoveredCell == null || cellHoverColor == null) return@drawBoard
        val cell = layout.board.cells[hoveredCell]

        backend.drawPolygon(
            shape = hoveredCell.toPixel().createHex(hexSize),
            color = cellHoverColor.withAlpha(48),
            outline = if (cell != null && (cell.highlight != null || cell.focused)) null else Stroke(cellHoverColor.withAlpha(140), 2f),
        )
    }
}
