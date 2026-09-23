package de.mineking.hexo.game.implementation.client

import de.mineking.hexo.game.implementation.client.profile.HmdProfileRepository
import de.mineking.hexo.game.model.RepositoryContainer
import de.mineking.hexo.game.model.formation.FormationRepository
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.leaderboard.LeaderboardRepository
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.tournament.TournamentRepository

interface HmdRepositoryContainer : RepositoryContainer {
    override val formationRepository: FormationRepository
    override val finishedGameRepository: FinishedGameRepository
    override val leaderboardRepository: LeaderboardRepository
    override val profileRepository: HmdProfileRepository
    override val sessionRepository: SessionRepository
    override val tournamentRepository: TournamentRepository
}
