package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.hds.implementation.HdsApiClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class FinishedGameRepositoryImpl(private val client: HdsApiClient) : FinishedGameRepository {
    private val requester = client.entityRequesterFactory.createEntityRequester<GameId, FinishedGameWithPosition> {
        val response = client.request("/finished-games/${it.value}")

        if (!response.status.isSuccess()) return@createEntityRequester null
        FinishedGameImpl(client, response.body())
    }

    private val listRequester = client.entityRequesterFactory.createEntityRequester<FinishedGamesParameter, List<FinishedGame>> { param ->
        val response = client.request(if (param.profile == null) "/finished-games" else "/profiles/${param.profile.value}/games") {
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
            .map { FinishedGameImpl(client, it) }
    }

    override suspend fun getGame(id: GameId) = requester.fetch(id)
    override suspend fun getHistory(page: Int, pageSize: Int, rated: Boolean?) =
        listRequester.fetch(FinishedGamesParameter(profile = null, page, pageSize, rated)) ?: emptyList()

    override suspend fun getProfileHistory(profile: ProfileId, page: Int, pageSize: Int, rated: Boolean?) =
        listRequester.fetch(FinishedGamesParameter(profile = profile, page, pageSize, rated)) ?: emptyList()

    data class FinishedGamesParameter(val profile: ProfileId?, val page: Int, val pageSize: Int, val rated: Boolean?)
}
