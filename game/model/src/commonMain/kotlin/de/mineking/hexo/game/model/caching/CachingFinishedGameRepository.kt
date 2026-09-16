package de.mineking.hexo.game.model.caching

import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.FinishedGameSelector
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.game.GameQueryError
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.InMemoryCache
import de.mineking.hexo.utils.types.Result

fun FinishedGameRepository.caching(
    config: CacheConfiguration<GameId, Result<FinishedGameWithPosition, GameQueryError>>,
): FinishedGameRepository = CachingFinishedGameRepository(this, config)

private class CachingFinishedGameRepository(
    val delegate: FinishedGameRepository,
    config: CacheConfiguration<GameId, Result<FinishedGameWithPosition, GameQueryError>>,
) : FinishedGameRepository {
    override val url by delegate::url

    private val cache = InMemoryCache(config)

    override suspend fun getGame(id: GameId) = cache.getOrPut(id) { delegate.getGame(it) }
    override suspend fun getGlobalHistory(selector: FinishedGameSelector) = delegate.getGlobalHistory(selector)
    override suspend fun getProfileHistory(profile: ProfileId, selector: FinishedGameSelector) =
        delegate.getProfileHistory(profile, selector)
}
