package de.mineking.hexo.game.model

import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.tournament.TournamentRepository

interface RepositoryContainer {
    val formationRepository: FormationRepository
    val finishedGameRepository: FinishedGameRepository
    val leaderboardRepository: LeaderboardRepository
    val profileRepository: ProfileRepository
    val sessionRepository: SessionRepository
    val tournamentRepository: TournamentRepository
}
