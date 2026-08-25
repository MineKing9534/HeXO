package de.mineking.hexo.board.render.image.svg

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.theme.Theme

class SvgBoardRenderer(
    private val padding: Int,
    private val layoutRadius: Double = 64.0,
    private val visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
    private val prettyPrint: Boolean = false,
) : BoardRenderer<Theme, String> {
    companion object {
        val Default = SvgBoardRenderer(padding = 32)
    }

    override suspend fun render(board: Board, param: Theme) = board.renderToSvg(padding, layoutRadius, visibleRadius, prettyPrint, param)
}
