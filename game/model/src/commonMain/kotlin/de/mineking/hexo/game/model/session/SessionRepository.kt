package de.mineking.hexo.game.model.session

import de.mineking.hexo.utils.types.EntityRepository
import de.mineking.hexo.utils.types.EntityState
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.QueryResult
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.Selector
import de.mineking.hexo.utils.types.SelectorFilter
import de.mineking.hexo.utils.types.adjustFilter
import kotlinx.coroutines.flow.StateFlow

sealed interface SessionQueryError : IError
data object SessionNotFoundError : SessionQueryError

data class SessionFilter(val rated: Boolean? = null) : SelectorFilter<Session>
typealias SessionSelector = Selector<Session, SessionFilter>

fun SessionSelector.rated(rated: Boolean?) = adjustFilter(::SessionFilter) { it.copy(rated = rated) }

interface SessionRepository : EntityRepository<Session> {
    val sessions: StateFlow<Map<SessionId, Session>>

    suspend fun getSessions(selector: SessionSelector = Selector): QueryResult<Session>
    suspend fun getSession(id: SessionId): Result<Session, SessionQueryError>
    fun observeSession(id: SessionId): StateFlow<EntityState<DetailedSession>>
}
