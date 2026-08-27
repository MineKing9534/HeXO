package de.mineking.hexo.game.model.game

import de.mineking.hexo.game.model.profile.ProfileId

interface FinishedGameRepository {
    suspend fun getGame(id: GameId): FinishedGameWithPosition?

    suspend fun getHistory(page: Int, pageSize: Int, rated: Boolean? = null): List<FinishedGame>
    suspend fun getProfileHistory(profile: ProfileId, page: Int, pageSize: Int, rated: Boolean? = null): List<FinishedGame>?
}
