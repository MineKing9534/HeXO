package de.mineking.hexo.board.render

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.board.Board

fun <P, R : Any> BoardRenderer<P, R>.caching(cacheSize: Long = 16): BoardRenderer<P, R> = when (this) {
    is CachingBoardRenderer -> this
    else -> CachingBoardRenderer(this, cacheSize)
}

private class CachingBoardRenderer<P, R : Any>(val delegate: BoardRenderer<P, R>, cacheSize: Long) : BoardRenderer<P, R> {
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<Pair<Board, P>, R>()

    override suspend fun render(board: Board, param: P) = cache.get(board to param) { delegate.render(board, param) }
}
