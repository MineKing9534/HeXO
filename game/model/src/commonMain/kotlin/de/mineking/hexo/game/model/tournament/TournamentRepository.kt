package de.mineking.hexo.game.model.tournament

import de.mineking.hexo.utils.types.EntityRepository
import de.mineking.hexo.utils.types.EntityState
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface TournamentQueryError : IError
data object TournamentNotFoundError : TournamentQueryError

interface TournamentRepository : EntityRepository<Tournament> {
    suspend fun getTournament(id: TournamentId): Result<Tournament, TournamentQueryError>
    fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>>

    companion object Empty : TournamentRepository {
        override val url = ""

        override suspend fun getTournament(id: TournamentId) = Result.Error(TournamentNotFoundError)
        override fun observeTournament(id: TournamentId) = MutableStateFlow(EntityState.NotFound).asStateFlow()
    }
}
