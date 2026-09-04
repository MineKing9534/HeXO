package de.mineking.hexo.hds.implementation.profile

import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.game.model.profile.ProfileNotFoundError
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.utils.EntityRequestException
import de.mineking.hexo.hds.implementation.utils.parseBodyOrNull
import de.mineking.hexo.utils.coroutines.awaitBoth
import de.mineking.hexo.utils.types.map
import de.mineking.hexo.utils.types.orNull
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class ProfileRepositoryImpl(internal val client: HdsApiClient) : ProfileRepository {
    override val url = "${client.host}/profile"

    private val statisticsRequester = client.entityRequesterFactory.createEntityRequester<ProfileId, ProfileStatistics?> { id ->
        val response = client.request("/profiles/${id.value}/statistics")

        @Serializable
        data class Response(val statistics: ProfileStatisticsDto)

        response.parseBodyOrNull<Response, ProfileStatistics> { it.statistics }
    }

    private val requester = client.entityRequesterFactory.createEntityRequester<ProfileId, ProfileWithStatistics?> { id ->
        awaitBoth(
            first = {
                client.request("/profiles/${id.value}")
                    .parseBodyOrNull<ProfileDto, ProfileDto> { it }
                    .successIfNotNullOrElse(ProfileNotFoundError)
            },
            second = { getProfileStatistics(id) },
        ).map { (profile, statistics) ->
            ProfileWithStatisticsImpl(client, profile, statistics)
        }.orNull()
    }

    private val searchRequester = client.entityRequesterFactory.createEntityRequester<String, List<Profile>> { name ->
        val response = client.request("/users/search") {
            parameter("q", name)
        }

        @Serializable
        data class Response(val users: List<ProfileDto>)

        if (!response.status.isSuccess()) throw EntityRequestException(response.bodyAsText())
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

    override suspend fun getProfilesByName(name: String) = searchRequester.fetch(name)
}
