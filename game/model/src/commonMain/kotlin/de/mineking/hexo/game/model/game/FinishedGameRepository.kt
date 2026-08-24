package de.mineking.hexo.game.model.game

interface FinishedGameRepository {
    suspend fun getGame(id: GameId): FinishedGameWithPosition?
    suspend fun getHistory(page: Int, pageSize: Int, rated: Boolean? = null): List<FinishedGame>
}
