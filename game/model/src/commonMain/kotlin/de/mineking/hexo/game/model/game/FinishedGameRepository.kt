package de.mineking.hexo.game.model.game

import de.mineking.hexo.game.model.EntityRepository
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileQueryError
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.QueryResult
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.Selector
import de.mineking.hexo.utils.types.SelectorFilter
import de.mineking.hexo.utils.types.adjustFilter

data class FinishedGameFilter(val rated: Boolean? = null) : SelectorFilter<FinishedGame>
typealias FinishedGameSelector = Selector<FinishedGame, FinishedGameFilter>

fun FinishedGameSelector.rated(rated: Boolean?) = adjustFilter(::FinishedGameFilter) { it.copy(rated = rated) }

sealed interface GameQueryError : IError
data object GameNotFoundError : GameQueryError

interface FinishedGameRepository : EntityRepository<FinishedGame> {
    suspend fun getGame(id: GameId): Result<FinishedGameWithPosition, GameQueryError>

    suspend fun getGlobalHistory(selector: FinishedGameSelector = Selector): QueryResult<FinishedGame>
    suspend fun getProfileHistory(
        profile: ProfileId,
        selector: FinishedGameSelector = Selector,
    ): Result<QueryResult<FinishedGame>, ProfileQueryError>
}
