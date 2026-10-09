package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.InMemoryCache

fun NotationParser.caching(config: CacheConfiguration<String, Board>): NotationParser = CachingNotationParser(this, config)

private class CachingNotationParser(
    val delegate: NotationParser,
    config: CacheConfiguration<String, Board>,
) : NotationParser {
    private val cache = InMemoryCache(config)

    override suspend fun parse(notation: String) = cache.getOrPut(notation) { delegate.parse(it) }
}
