package de.mineking.hexo.game.model

import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.tournament.TournamentRepository

interface RepositoryWrapper {
    fun ProfileRepository.wrap(): ProfileRepository
    fun LeaderboardRepository.wrap(): LeaderboardRepository
    fun FinishedGameRepository.wrap(): FinishedGameRepository
    fun TournamentRepository.wrap(): TournamentRepository
    fun FormationRepository.wrap(): FormationRepository
    fun SessionRepository.wrap(): SessionRepository

    companion object : RepositoryWrapper {
        override fun ProfileRepository.wrap() = this
        override fun LeaderboardRepository.wrap() = this
        override fun FinishedGameRepository.wrap() = this
        override fun TournamentRepository.wrap() = this
        override fun FormationRepository.wrap() = this
        override fun SessionRepository.wrap() = this
    }
}
