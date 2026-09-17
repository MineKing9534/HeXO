package de.mineking.hexo.board.render

import de.mineking.hexo.board.Board
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.InMemoryCache

fun <P, R : Any> BoardRenderer<P, R>.caching(config: CacheConfiguration<Any?, R>): BoardRenderer<P, R> = CachingBoardRenderer(this, config)

private class CachingBoardRenderer<P, R : Any>(
    val delegate: BoardRenderer<P, R>,
    config: CacheConfiguration<Any?, R>,
) : BoardRenderer<P, R> {
    private val cache = InMemoryCache<Pair<Board, P>, R>(config)

    override suspend fun render(board: Board, param: P) = cache.getOrPut(board to param) {
        delegate.render(board, param)
    }
}
