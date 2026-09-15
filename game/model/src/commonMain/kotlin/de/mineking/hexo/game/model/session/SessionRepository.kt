package de.mineking.hexo.game.model.session

import de.mineking.hexo.game.model.EntityRepository
import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import kotlinx.coroutines.flow.StateFlow

sealed interface SessionQueryError : IError
data object SessionNotFoundError : SessionQueryError

interface SessionRepository : EntityRepository<Session> {
    val sessions: StateFlow<Map<SessionId, Session>>

    suspend fun getSession(id: SessionId): Result<Session, SessionQueryError>
    fun observeSession(id: SessionId): StateFlow<EntityState<DetailedSession>>
}
