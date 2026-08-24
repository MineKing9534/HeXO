package de.mineking.hexo.hds.implementation.profile

import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.coroutines.awaitBothOrNull
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class ProfileRepositoryImpl(internal val client: HdsApiClient) : ProfileRepository {
    private val statisticsRequester = client.entityRequesterFactory.createEntityRequester<ProfileId, ProfileStatistics> { id ->
        val response = client.request("/profiles/${id.value}/statistics")

        @Serializable
        data class Response(val statistics: ProfileStatisticsDto)

        if (!response.status.isSuccess()) return@createEntityRequester null
        response.body<Response>().statistics
    }

    private val requester = client.entityRequesterFactory.createEntityRequester<ProfileId, ProfileWithStatistics> { id ->
        val (profile, statistics) = awaitBothOrNull(
            first = { client.request("/profiles/${id.value}").takeIf { it.status.isSuccess() }?.body<ProfileDto>() },
            second = { getProfileStatistics(id) },
        ) ?: return@createEntityRequester null

        ProfileWithStatisticsImpl(client, profile, statistics)
    }

    private val searchRequester = client.entityRequesterFactory.createEntityRequester<String, List<Profile>> { name ->
        val response = client.request("/users/search") {
            parameter("q", name)
        }

        @Serializable
        data class Response(val users: List<ProfileDto>)

        if (!response.status.isSuccess()) return@createEntityRequester null
        response.body<Response>().users.map {
            ProfileImpl(client, it)
        }
    }

    override suspend fun getProfileStatistics(id: ProfileId) = statisticsRequester.fetch(id)
    override suspend fun getProfile(id: ProfileId) = requester.fetch(id)

    override suspend fun getProfilesByName(name: String) = searchRequester.fetch(name) ?: emptyList()
}
