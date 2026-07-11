package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.compose.BoardInteraction
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.hds.AbstractGamePosition
import de.mineking.hexo.hds.asBoard
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.w3c.dom.HTMLCanvasElement

@Composable
fun GamePositionBoardPane(
    position: AbstractGamePosition,
    move: Int,
    overlay: Board,
    readOnly: Boolean,
    viewport: BoardViewport?,
    onViewportChange: (BoardViewport) -> Unit,
    onBoardInteraction: (BoardInteraction) -> Unit,
    analyseAs: AnalyserTurn? = null,
    attrs: AttrBuilderContext<HTMLCanvasElement>? = null,
    content: BoardPaneContentBuilder? = null,
) {
    val board = remember(move, position) { position.asBoard(move) }
    val transformedBoard = remember(board, overlay) {
        (board + overlay).focusWinningRows()
    }

    BoardPane(
        board = transformedBoard,
        readOnly = readOnly,
        viewport = viewport,
        onViewportChange = onViewportChange,
        onBoardInteraction = onBoardInteraction,
        analyseAs = analyseAs,
        attrs = attrs,
        content = content,
    )
}
