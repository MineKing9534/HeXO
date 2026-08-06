package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.model.game.FinishedGame
import de.mineking.hexo.hds.model.game.FinishedGameRepository
import de.mineking.hexo.hds.model.game.GameId
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class FinishedGameRepositoryImpl(private val client: HdsApiClient) : FinishedGameRepository {
    private val requester = client.entityRequesterFactory.createEntityRequester<GameId, FinishedGame> {
        val response = client.request("/finished-games/${it.value}")

        if (!response.status.isSuccess()) return@createEntityRequester null
        FinishedGameImpl.of(client, response.body())
    }

    private val listRequester = client.entityRequesterFactory.createEntityRequester<FinishedGamesParameter, List<FinishedGame>> { param ->
        val response = client.request("/finished-games") {
            parameter("page", param.page)
            parameter("pageSize", param.pageSize)
            parameter("rated", when (param.rated) {
                true -> "rated"
                false -> "unrated"
                else -> "all"
            })
        }

        if (!response.status.isSuccess()) return@createEntityRequester null

        @Serializable
        data class Response(val games: List<FinishedGameDto>)
        response.body<Response>().games
            .map { FinishedGameImpl.of(client, it) }
    }

    override suspend fun getGame(id: GameId) = requester.fetch(id)
    override suspend fun getFinishedGames(page: Int, pageSize: Int, rated: Boolean?) =
        listRequester.fetch(FinishedGamesParameter(page, pageSize, rated)) ?: emptyList()

    data class FinishedGamesParameter(val page: Int, val pageSize: Int, val rated: Boolean?)
}
