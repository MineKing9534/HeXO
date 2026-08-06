package de.mineking.hexo.hds.implementation.caching

import de.mineking.hexo.hds.implementation.RepositoryWrapper
import de.mineking.hexo.hds.model.formation.FormationRepository
import de.mineking.hexo.hds.model.game.FinishedGameRepository
import de.mineking.hexo.hds.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.hds.model.profile.ProfileRepository
import de.mineking.hexo.hds.model.session.SessionRepository
import de.mineking.hexo.hds.model.tournament.TournamentRepository

private const val DEFAULT_CACHE_SIZE = 16L

class CachingRepositoryWrapper(val cacheSize: Long = DEFAULT_CACHE_SIZE) : RepositoryWrapper {
    override fun ProfileRepository.wrap(): ProfileRepository = CachingProfileRepository(this, cacheSize)
    override fun LeaderboardRepository.wrap(): LeaderboardRepository = CachingLeaderboardRepository(this)
    override fun FinishedGameRepository.wrap(): FinishedGameRepository = CachingFinishedGameRepository(this, cacheSize)
    override fun TournamentRepository.wrap(): TournamentRepository = CachingTournamentRepository(this, cacheSize)
    override fun FormationRepository.wrap(): FormationRepository = CachingFormationRepository(this, cacheSize)
    override fun SessionRepository.wrap() = this
}
