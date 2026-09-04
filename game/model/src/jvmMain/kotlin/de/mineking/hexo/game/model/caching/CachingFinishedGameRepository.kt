package de.mineking.hexo.game.model.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.FinishedGameSelector
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.game.GameQueryError
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.utils.types.Result

internal class CachingFinishedGameRepository(val delegate: FinishedGameRepository, cacheSize: Long) : FinishedGameRepository {
    override val url by delegate::url

    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<GameId, Result<FinishedGameWithPosition, GameQueryError>>()

    override suspend fun getGame(id: GameId) = cache.get(id) { delegate.getGame(it) }
    override suspend fun getGlobalHistory(selector: FinishedGameSelector) = delegate.getGlobalHistory(selector)
    override suspend fun getProfileHistory(profile: ProfileId, selector: FinishedGameSelector) =
        delegate.getProfileHistory(profile, selector)
}
