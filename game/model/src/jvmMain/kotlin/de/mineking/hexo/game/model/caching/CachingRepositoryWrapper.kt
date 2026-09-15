package de.mineking.hexo.game.model.caching

import de.mineking.hexo.game.model.RepositoryWrapper
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.tournament.TournamentRepository

private const val DEFAULT_CACHE_SIZE = 16L

class CachingRepositoryWrapper(val cacheSize: Long = DEFAULT_CACHE_SIZE) : RepositoryWrapper {
    override fun ProfileRepository.wrap(): ProfileRepository = CachingProfileRepository(this, cacheSize)
    override fun LeaderboardRepository.wrap(): LeaderboardRepository = CachingLeaderboardRepository(this)
    override fun FinishedGameRepository.wrap(): FinishedGameRepository = CachingFinishedGameRepository(this, cacheSize)
    override fun TournamentRepository.wrap(): TournamentRepository = CachingTournamentRepository(this, cacheSize)
    override fun FormationRepository.wrap(): FormationRepository = CachingFormationRepository(this, cacheSize)
    override fun SessionRepository.wrap() = this
}
