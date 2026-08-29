package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.FinishedGameSelector
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.game.GameNotFoundError
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileNotFoundError
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.types.QueryResult
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class FinishedGameRepositoryImpl(private val client: HdsApiClient) : FinishedGameRepository {
    override val url = "${client.host}/games"

    private val requester = client.entityRequesterFactory.createEntityRequester<GameId, FinishedGameWithPosition> {
        val response = client.request("/finished-games/${it.value}")

        if (!response.status.isSuccess()) return@createEntityRequester null
        FinishedGameImpl(client, response.body())
    }

    private val listRequester = client.entityRequesterFactory.createEntityRequester<FinishedGamesParameter, QueryResult<FinishedGame>> { param ->
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
        data class PaginationInfo(val totalGames: Int)

        @Serializable
        data class Response(val games: List<FinishedGameDto>, val pagination: PaginationInfo)

        val result = response.body<Response>()
        QueryResult(
            entries = result.games.map { FinishedGameImpl(client, it) },
            totalCount = result.pagination.totalGames,
        )
    }

    override suspend fun getGame(id: GameId) = requester.fetch(id)
        .successIfNotNullOrElse(GameNotFoundError)
    private suspend fun getHistory(profile: ProfileId?, selector: FinishedGameSelector): QueryResult<FinishedGame>? {
        val (offset, limit, filter) = selector

        require((offset == null) == (limit == null))
        val (page, pageSize) = if (offset != null && limit != null) {
            require(offset % limit == 0)

            val page = offset / limit + 1
            page to limit
        } else {
            null to null
        }

        return listRequester.fetch(FinishedGamesParameter(profile, page, pageSize, filter?.rated))
    }

    override suspend fun getGlobalHistory(selector: FinishedGameSelector) = getHistory(null, selector)
        ?: QueryResult.Empty

    override suspend fun getProfileHistory(profile: ProfileId, selector: FinishedGameSelector) = getHistory(profile, selector)
        .successIfNotNullOrElse(ProfileNotFoundError)

    data class FinishedGamesParameter(val profile: ProfileId?, val page: Int?, val pageSize: Int?, val rated: Boolean?)
}
