package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.FinishedGameFilter
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.FinishedGameSelector
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.game.GameNotFoundError
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileNotFoundError
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.utils.createPaginated
import de.mineking.hexo.hds.implementation.utils.parseBodyOrNull
import de.mineking.hexo.utils.types.QueryResultDto
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

internal class FinishedGameRepositoryImpl(private val client: HdsApiClient) : FinishedGameRepository {
    override val url = "${client.publicUrl}/games"

    private val requester = client.entityRequesterFactory.createEntityRequester<GameId, FinishedGameWithPosition?> { id ->
        val response = client.request("/finished-games/${id.value}")
        response.parseBodyOrNull<FinishedGameDto, FinishedGameWithPosition> {
            FinishedGameImpl(client, it)
        }
    }

    private val listRequester = client.entityRequesterFactory.createPaginated<ProfileId?, FinishedGame, FinishedGameFilter>(
        client = client,
        path = { profile -> if (profile == null) "/finished-games" else "/profiles/${profile.value}/games" },
        config = {
            parameter("rated", when (it?.rated) {
                true -> "rated"
                false -> "unrated"
                else -> "all"
            })
        },
    ) { response ->
        @Serializable
        data class PaginationInfo(val totalGames: Int)

        @Serializable
        data class Response(val games: List<FinishedGameDto>, val pagination: PaginationInfo)

        if (response.status == HttpStatusCode.Unauthorized) return@createPaginated null

        response.parseBodyOrNull<Response, QueryResultDto<FinishedGame>> { result ->
            QueryResultDto(
                entries = result.games.map { FinishedGameImpl(client, it) },
                totalCount = result.pagination.totalGames,
            )
        } ?: throw ProfileNotFoundException()
    }

    override suspend fun getGame(id: GameId) = requester.fetch(id)
        .successIfNotNullOrElse(GameNotFoundError)
    private suspend fun getHistory(profile: ProfileId?, selector: FinishedGameSelector) = listRequester.fetchPaginated(profile, selector)

    override suspend fun getGlobalHistory(selector: FinishedGameSelector) = getHistory(null, selector)

    override suspend fun getProfileHistory(profile: ProfileId, selector: FinishedGameSelector) = try {
        Result.Success(getHistory(profile, selector))
    } catch (_: ProfileNotFoundException) {
        Result.Error(ProfileNotFoundError)
    }

    private class ProfileNotFoundException : RuntimeException()
}
