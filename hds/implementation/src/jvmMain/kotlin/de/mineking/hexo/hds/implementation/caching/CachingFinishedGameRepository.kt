package de.mineking.hexo.hds.implementation.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.hds.model.game.FinishedGame
import de.mineking.hexo.hds.model.game.FinishedGameRepository
import de.mineking.hexo.hds.model.game.GameId

internal class CachingFinishedGameRepository(val delegate: FinishedGameRepository, cacheSize: Long) : FinishedGameRepository {
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<GameId, FinishedGame>()

    override suspend fun getGame(id: GameId) = cache.getOrNull(id) { delegate.getGame(it) }
    override suspend fun getFinishedGames(page: Int, pageSize: Int, rated: Boolean?) = delegate.getFinishedGames(page, pageSize, rated)
}
