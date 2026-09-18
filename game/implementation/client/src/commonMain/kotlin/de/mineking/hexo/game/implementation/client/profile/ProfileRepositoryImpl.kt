package de.mineking.hexo.game.implementation.client.profile

import de.mineking.hexo.game.implementation.client.HexoGameClient
import de.mineking.hexo.game.implementation.protocol.ProfileDto
import de.mineking.hexo.game.implementation.protocol.ProfileStatisticsDto
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.game.model.profile.ProfileQueryError
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.map
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments

interface HmdProfileRepository : ProfileRepository {
    override suspend fun getProfile(id: ProfileIdentifier): Result<HmdProfileWithStatistics, ProfileQueryError>
    override suspend fun getProfilesByName(name: String): List<HmdProfile>
}

suspend fun HmdProfileRepository.getProfileById(id: ProfileId) = getProfile(ProfileIdentifier.Id(id))
suspend fun HmdProfileRepository.getProfileByName(name: String) = getProfile(ProfileIdentifier.Name(name))

internal class ProfileRepositoryImpl(
    private val client: HexoGameClient,
) : ProfileRepository {
    override val url = "${client.publicUrl}/profiles"

    private val requester = client.entityRequesterFactory
        .createEntityRequester<ProfileIdentifier, Result<HmdProfileWithStatistics, ProfileQueryError>> { id ->
            client.request("/profiles") {
                url {
                    when (id) {
                        is ProfileIdentifier.Id -> appendPathSegments(id.id.value)
                        is ProfileIdentifier.Name -> appendPathSegments("by-name", id.name)
                    }
                }
            }.body<Result<ProfileDto, ProfileQueryError>>()
                .map { ProfileImpl(client, it).withStatistics() }
        }

    private val statisticsRequester = client.entityRequesterFactory
        .createEntityRequester<ProfileIdentifier, Result<ProfileStatistics, ProfileQueryError>> { id ->
            client.request("/profiles") {
                url {
                    when (id) {
                        is ProfileIdentifier.Id -> appendPathSegments(id.id.value)
                        is ProfileIdentifier.Name -> appendPathSegments("by-name", id.name)
                    }

                    appendPathSegments("statistics")
                }
            }.body<Result<ProfileStatisticsDto, ProfileQueryError>>()
        }

    private val searchRequester = client.entityRequesterFactory.createEntityRequester<String, List<HmdProfile>> { name ->
        if (name.isBlank()) return@createEntityRequester emptyList()

        client.request("/profiles") { parameter("name", name) }
            .body<List<ProfileDto>>()
            .map { ProfileImpl(client, it) }
    }

    override suspend fun getProfile(id: ProfileIdentifier) = requester.fetch(id)
    override suspend fun getProfileStatistics(id: ProfileIdentifier) = statisticsRequester.fetch(id)

    override suspend fun getProfilesByName(name: String) = searchRequester.fetch(name)
}
