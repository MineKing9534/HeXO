package de.mineking.hexo.hds.implementation.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameId

internal class CachingFinishedGameRepository(val delegate: FinishedGameRepository, cacheSize: Long) : FinishedGameRepository {
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<GameId, FinishedGameWithPosition>()

    override suspend fun getGame(id: GameId) = cache.getOrNull(id) { delegate.getGame(it) }
    override suspend fun getHistory(page: Int, pageSize: Int, rated: Boolean?) = delegate.getHistory(page, pageSize, rated)
}
