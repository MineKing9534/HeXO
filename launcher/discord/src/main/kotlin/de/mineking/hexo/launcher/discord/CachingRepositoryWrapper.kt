package de.mineking.hexo.launcher.discord

import de.mineking.hexo.game.model.RepositoryWrapper
import de.mineking.hexo.game.model.caching.caching
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.tournament.TournamentRepository
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.EvictionStrategy
import de.mineking.hexo.utils.cache.ExpirationStrategy
import de.mineking.hexo.utils.cache.entries
import kotlin.time.Duration.Companion.minutes

private val defaultCacheConfig = CacheConfiguration(
    sizeLimit = 16.entries,
    evictionStrategy = EvictionStrategy.LeastFrequentlyUsed,
    expiration = null,
)

object CachingRepositoryWrapper : RepositoryWrapper {
    override fun ProfileRepository.wrap() = caching(
        profileConfig = defaultCacheConfig.copy(expiration = ExpirationStrategy.AfterWrite(3.minutes)),
        searchConfig = defaultCacheConfig.copy(expiration = ExpirationStrategy.AfterWrite(10.minutes)),
    )
    override fun LeaderboardRepository.wrap() = caching()
    override fun FinishedGameRepository.wrap() = caching(defaultCacheConfig)
    override fun TournamentRepository.wrap() = caching(defaultCacheConfig)
    override fun FormationRepository.wrap() = caching(defaultCacheConfig)
    override fun SessionRepository.wrap() = this
}
