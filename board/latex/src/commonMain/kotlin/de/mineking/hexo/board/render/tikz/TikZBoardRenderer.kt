package de.mineking.hexo.board.render.tikz

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.theme.Theme

class TikZBoardRenderer(
    private val padding: Int,
    private val layoutRadius: Double = 64.0,
    private val visibleRadius: Int = DEFAULT_VISIBLE_RADIUS,
) : BoardRenderer<Theme, String> {
    companion object {
        val Default = TikZBoardRenderer(padding = 32)
    }

    override suspend fun render(board: Board, param: Theme) = board.renderToTikZ(padding, layoutRadius, visibleRadius, param)
}
