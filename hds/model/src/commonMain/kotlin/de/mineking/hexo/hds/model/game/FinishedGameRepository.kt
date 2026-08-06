package de.mineking.hexo.hds.model.game

interface FinishedGameRepository {
    suspend fun getGame(id: GameId): FinishedGame?

    suspend fun getFinishedGames(page: Int, pageSize: Int, rated: Boolean? = null): List<FinishedGame>
}
