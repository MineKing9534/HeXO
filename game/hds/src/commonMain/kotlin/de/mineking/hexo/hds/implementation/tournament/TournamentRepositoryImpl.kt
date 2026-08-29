package de.mineking.hexo.hds.implementation.tournament

import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.game.model.tournament.Tournament
import de.mineking.hexo.game.model.tournament.TournamentId
import de.mineking.hexo.game.model.tournament.TournamentNotFoundError
import de.mineking.hexo.game.model.tournament.TournamentRepository
import de.mineking.hexo.game.model.tournament.isTerminal
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.socket.TournamentUpdate
import de.mineking.hexo.hds.implementation.socket.listen
import de.mineking.hexo.hds.implementation.utils.withLock
import de.mineking.hexo.utils.types.isSuccess
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class TournamentRepositoryImpl(private val client: HdsApiClient) : TournamentRepository {
    override val url = "${client.host}/tournaments"

    init {
        client.socketClient?.listen<TournamentUpdate> { event ->
            client.coroutineScope.launch {
                val _ = getTournament(event.tournamentId)
            }
        }
    }

    private val cacheLock = SynchronizedObject()
    private val cache = mutableMapOf<TournamentId, MutableStateFlow<EntityState<Tournament>>>()

    private val requester = client.entityRequesterFactory.createEntityRequester<TournamentId, Tournament> { id ->
        val response = client.request("/tournaments/${id.value}")
        val tournament = when {
            response.status.isSuccess() -> TournamentImpl(client, response.body())
            else -> null
        }

        cacheLock.withLock {
            val state = tournament?.let { EntityState.Data(it) } ?: EntityState.NotFound
            cache[id]?.value = state

            if (tournament == null || tournament.status.isTerminal()) {
                cache -= id
            }
        }

        tournament
    }

    override suspend fun getTournament(id: TournamentId) = requester.fetch(id).successIfNotNullOrElse(TournamentNotFoundError)

    override fun observeTournament(id: TournamentId): StateFlow<EntityState<Tournament>> {
        if (client.socketClient == null) error("Cannot observe tournaments without a SocketIO connection")

        var shouldStartFetch = false
        val flow = cacheLock.withLock {
            cache.getOrPut(id) {
                shouldStartFetch = true
                MutableStateFlow(EntityState.Loading)
            }
        }

        if (shouldStartFetch) {
            client.coroutineScope.launch {
                val tournament = getTournament(id)
                cacheLock.withLock {
                    flow.value = when {
                        tournament.isSuccess() -> EntityState.Data(tournament.value)
                        else -> EntityState.NotFound
                    }

                    if (!tournament.isSuccess() || tournament.value.status.isTerminal()) {
                        cache -= id
                    }
                }
            }
        }

        return flow
    }
}
