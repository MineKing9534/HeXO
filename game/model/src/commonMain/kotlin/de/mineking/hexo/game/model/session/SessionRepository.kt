package de.mineking.hexo.game.model.session

import de.mineking.hexo.game.model.EntityState
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val lobbies: StateFlow<Map<SessionId, LobbySession>>

    fun observeSession(id: SessionId): StateFlow<EntityState<Session>>
}
