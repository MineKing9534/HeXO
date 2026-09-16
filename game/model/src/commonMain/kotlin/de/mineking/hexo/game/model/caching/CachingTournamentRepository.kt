package de.mineking.hexo.game.model.caching

import de.mineking.hexo.game.model.tournament.Tournament
import de.mineking.hexo.game.model.tournament.TournamentId
import de.mineking.hexo.game.model.tournament.TournamentQueryError
import de.mineking.hexo.game.model.tournament.TournamentRepository
import de.mineking.hexo.game.model.tournament.isTerminal
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.InMemoryCache
import de.mineking.hexo.utils.types.EntityState
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun TournamentRepository.caching(
    config: CacheConfiguration<TournamentId, Tournament>,
): TournamentRepository = CachingTournamentRepository(this, config)

private class CachingTournamentRepository(
    val delegate: TournamentRepository,
    config: CacheConfiguration<TournamentId, Tournament>,
) : TournamentRepository {
    override val url by delegate::url

    private val cache = InMemoryCache(config)

    override suspend fun getTournament(id: TournamentId): Result<Tournament, TournamentQueryError> {
        cache.getIfAvailable(id)?.let { return Result.Success(it) }

        return delegate.getTournament(id).also {
            if (it.isSuccess() && it.value.status.isTerminal()) {
                cache.put(id, it.value)
            }
        }
    }

    override fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>> {
        cache.getIfAvailable(id)?.let { return MutableStateFlow(EntityState.Data(it)) }
        return delegate.observeTournament(id)
    }
}
