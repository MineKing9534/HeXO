package de.mineking.hexo.game.model.tournament

import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import kotlinx.coroutines.flow.StateFlow

sealed interface TournamentQueryError : IError
data object TournamentNotFoundError : TournamentQueryError

interface TournamentRepository {
    suspend fun getTournament(id: TournamentId): Result<Tournament, TournamentQueryError>
    fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>>
}
