package de.mineking.hexo.game.implementation.client

import de.mineking.hexo.game.implementation.client.profile.HmdProfileRepository
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.tournament.TournamentRepository

interface HmdRepositoryWrapper {
    fun HmdProfileRepository.wrap(): HmdProfileRepository
    fun LeaderboardRepository.wrap(): LeaderboardRepository
    fun FinishedGameRepository.wrap(): FinishedGameRepository
    fun TournamentRepository.wrap(): TournamentRepository
    fun FormationRepository.wrap(): FormationRepository
    fun SessionRepository.wrap(): SessionRepository

    companion object : HmdRepositoryWrapper {
        override fun HmdProfileRepository.wrap() = this
        override fun LeaderboardRepository.wrap() = this
        override fun FinishedGameRepository.wrap() = this
        override fun TournamentRepository.wrap() = this
        override fun FormationRepository.wrap() = this
        override fun SessionRepository.wrap() = this
    }
}
