package de.mineking.hexo.game.model.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.game.model.tournament.Tournament
import de.mineking.hexo.game.model.tournament.TournamentId
import de.mineking.hexo.game.model.tournament.TournamentQueryError
import de.mineking.hexo.game.model.tournament.TournamentRepository
import de.mineking.hexo.game.model.tournament.isTerminal
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class CachingTournamentRepository(val delegate: TournamentRepository, cacheSize: Long) : TournamentRepository {
    override val url by delegate::url

    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<TournamentId, Tournament>()

    override suspend fun getTournament(id: TournamentId): Result<Tournament, TournamentQueryError> {
        cache.getIfPresent(id)?.let { return Result.Success(it) }

        return delegate.getTournament(id).also {
            if (it.isSuccess() && it.value.status.isTerminal()) {
                cache.put(id, it.value)
            }
        }
    }

    override fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>> {
        cache.getOrNull(id)?.let { return MutableStateFlow(EntityState.Data(it)) }
        return delegate.observeTournament(id)
    }
}
