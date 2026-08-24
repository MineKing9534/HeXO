package de.mineking.hexo.hds.implementation.caching

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.game.model.tournament.Tournament
import de.mineking.hexo.game.model.tournament.TournamentId
import de.mineking.hexo.game.model.tournament.TournamentRepository
import de.mineking.hexo.game.model.tournament.isTerminal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class CachingTournamentRepository(val delegate: TournamentRepository, cacheSize: Long) : TournamentRepository {
    private val cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .asCache<TournamentId, Tournament>()

    override suspend fun getTournament(id: TournamentId): Tournament? {
        cache.getIfPresent(id)?.let { return it }

        return delegate.getTournament(id)?.also {
            if (it.status.isTerminal()) {
                cache.put(id, it)
            }
        }
    }

    override fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>> {
        cache.getOrNull(id)?.let { return MutableStateFlow(EntityState.Data(it)) }
        return delegate.observeTournament(id)
    }
}
