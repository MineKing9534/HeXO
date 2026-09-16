package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.InMemoryCache

fun BoardParser.caching(config: CacheConfiguration<String, Board>): BoardParser = CachingBoardParser(this, config)

private class CachingBoardParser(
    val delegate: BoardParser,
    config: CacheConfiguration<String, Board>,
) : BoardParser {
    private val cache = InMemoryCache(config)

    override suspend fun parse(notation: String) = cache.getOrPut(notation) { delegate.parse(it) }
}
