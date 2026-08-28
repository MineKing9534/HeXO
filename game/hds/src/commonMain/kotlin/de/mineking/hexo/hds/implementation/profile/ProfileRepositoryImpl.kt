package de.mineking.hexo.hds.implementation.profile

import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.game.model.profile.ProfileNotFoundError
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.coroutines.awaitBoth
import de.mineking.hexo.utils.types.orElse
import de.mineking.hexo.utils.types.successIfNotNullOrElse
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
        val (profile, statistics) = awaitBoth(
            first = {
                client.request("/profiles/${id.value}")
                    .takeIf { it.status.isSuccess() }
                    ?.body<ProfileDto>()
                    .successIfNotNullOrElse(ProfileNotFoundError)
            },
            second = { getProfileStatistics(id) },
        ).orElse { return@createEntityRequester null }

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

    override suspend fun getProfileStatistics(id: ProfileId) = statisticsRequester.fetch(id).successIfNotNullOrElse(ProfileNotFoundError)
    override suspend fun getProfile(id: ProfileIdentifier) = when (id) {
        is ProfileIdentifier.Id -> requester.fetch(id.id)
        is ProfileIdentifier.Name -> getProfilesByName(id.name)
            .firstOrNull { it.displayName == id.name }
            ?.withStatistics()
    }.successIfNotNullOrElse(ProfileNotFoundError)

    override suspend fun getProfilesByName(name: String) = searchRequester.fetch(name) ?: emptyList()
}
