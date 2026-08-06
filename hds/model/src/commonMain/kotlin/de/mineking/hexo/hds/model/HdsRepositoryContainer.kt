package de.mineking.hexo.hds.model

import de.mineking.hexo.hds.model.formation.FormationRepository
import de.mineking.hexo.hds.model.game.FinishedGameRepository
import de.mineking.hexo.hds.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.hds.model.profile.ProfileRepository
import de.mineking.hexo.hds.model.session.SessionRepository
import de.mineking.hexo.hds.model.tournament.TournamentRepository

interface HdsRepositoryContainer {
    val formationRepository: FormationRepository
    val finishedGameRepository: FinishedGameRepository
    val leaderboardRepository: LeaderboardRepository
    val profileRepository: ProfileRepository
    val sessionRepository: SessionRepository
    val tournamentRepository: TournamentRepository
}
