package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.InMemoryCache
import de.mineking.hexo.utils.cache.mapKeys

fun NotationParser.caching(config: CacheConfiguration<String, Board>): NotationParser = CachingNotationParser(this, config)

private data class NotationCacheKey(val notation: String, val isInMergeContext: Boolean)

private class CachingNotationParser(
    val delegate: NotationParser,
    config: CacheConfiguration<String, Board>,
) : NotationParser {
    private val cache = InMemoryCache(config.mapKeys { key: NotationCacheKey -> key.notation })

    override suspend fun parse(notation: String): Board {
        val key = NotationCacheKey(notation, isInMergeContext())
        return cache.getOrPut(key) { delegate.parse(it.notation) }
    }
}
