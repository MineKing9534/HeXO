package de.mineking.hexo.game.implementation.client.profile

import de.mineking.hexo.game.implementation.client.HmdApiClient
import de.mineking.hexo.game.implementation.client.isAuthenticationError
import de.mineking.hexo.game.implementation.protocol.ProfileDto
import de.mineking.hexo.game.implementation.protocol.ProfileStatisticsDto
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.game.model.profile.ProfileNotFoundError
import de.mineking.hexo.game.model.profile.ProfileQueryError
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.utils.types.EntityState
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.map
import de.mineking.hexo.utils.types.orElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val logger = KotlinLogging.logger {}

interface HmdProfileRepository : ProfileRepository {
    val currentProfile: StateFlow<EntityState<HmdProfile>>

    override suspend fun getProfile(id: ProfileIdentifier): Result<HmdProfileWithStatistics, ProfileQueryError>
    override suspend fun getProfilesByName(name: String): List<HmdProfile>
}

suspend fun HmdProfileRepository.getProfileByName(name: String) = getProfile(ProfileIdentifier.Name(name))

internal class ProfileRepositoryImpl(
    private val client: HmdApiClient,
) : HmdProfileRepository {
    private val currentProfileUpdateLock = Mutex()

    override val url = "${client.publicUrl}/profiles"

    override val currentProfile: StateFlow<EntityState<HmdProfile>>
        field = MutableStateFlow<EntityState<HmdProfile>>(EntityState.Loading)

    init {
        client.client.httpClient.launch {
            updateCurrentProfile()
        }
    }

    private val requester = client.entityRequesterFactory
        .createEntityRequester<ProfileIdentifier, Result<HmdProfileWithStatistics, ProfileQueryError>> { id ->
            client.request("/profiles") {
                url {
                    when (id) {
                        is ProfileId -> appendPathSegments(id.value)
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
                        is ProfileId -> appendPathSegments(id.value)
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

    private suspend fun fetchCurrentProfile(): EntityState<HmdProfile> {
        val response = client.request("/profiles/@me")
        if (response.status.isAuthenticationError()) return EntityState.NotFound

        return response.body<Result<ProfileDto, ProfileNotFoundError>>()
            .map { EntityState.Data(ProfileImpl(client, it)) }
            .orElse { EntityState.NotFound }
    }

    internal suspend fun updateCurrentProfile() {
        currentProfileUpdateLock.withLock {
            currentProfile.value = EntityState.Loading

            @Suppress("TooGenericExceptionCaught")
            try {
                currentProfile.value = fetchCurrentProfile()
            } catch (e: Exception) {
                logger.warn(e) { "Failed to fetch current profile" }
                currentProfile.value = EntityState.NotFound
            }
        }
    }

    override suspend fun getProfile(id: ProfileIdentifier) = requester.fetch(id)
    override suspend fun getProfileStatistics(id: ProfileIdentifier) = statisticsRequester.fetch(id)

    override suspend fun getProfilesByName(name: String) = searchRequester.fetch(name)
}
