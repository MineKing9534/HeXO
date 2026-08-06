package de.mineking.hexo.hds.model.tournament

import de.mineking.hexo.hds.model.EntityState
import kotlinx.coroutines.flow.StateFlow

interface TournamentRepository {
    suspend fun getTournament(id: TournamentId): Tournament?
    fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>>
}
