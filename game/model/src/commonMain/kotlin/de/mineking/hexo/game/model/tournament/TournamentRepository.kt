package de.mineking.hexo.game.model.tournament

import de.mineking.hexo.game.model.EntityState
import kotlinx.coroutines.flow.StateFlow

interface TournamentRepository {
    suspend fun getTournament(id: TournamentId): Tournament?
    fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>>
}
