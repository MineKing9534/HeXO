package de.mineking.hexo.hds.model.session

import de.mineking.hexo.hds.model.EntityState
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val lobbies: StateFlow<Map<SessionId, LobbySession>>

    fun observeSession(id: SessionId): StateFlow<EntityState<Session>>
}
