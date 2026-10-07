package de.mineking.hexo.game.implementation.client.profile

import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.game.implementation.client.HmdApiClient
import de.mineking.hexo.game.implementation.protocol.ProfileDto
import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.FinishedGameSelector
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.utils.types.EntityNotFoundException
import de.mineking.hexo.utils.types.QueryResult
import de.mineking.hexo.utils.types.orThrow
import de.mineking.hexo.utils.types.urlOf

interface HmdProfile : Profile {
    val discord: DiscordUserId?

    override suspend fun retrieveStatistics(forceUpdate: Boolean): ProfileStatistics
    override suspend fun withStatistics(forceUpdate: Boolean): HmdProfileWithStatistics

    override suspend fun retrieveGames(selector: FinishedGameSelector): QueryResult<FinishedGame>
}

interface HmdProfileWithStatistics : HmdProfile, ProfileWithStatistics

internal class ProfileImpl(
    private val client: HmdApiClient,
    private val dto: ProfileDto,
) : HmdProfile {
    override val id = dto.id
    override val discord = dto.discord
    override val url = client.profileRepository.urlOf(id)
    override val displayName = dto.displayName
    override val image = dto.image
    override val registeredAt = dto.registeredAt

    override suspend fun retrieveStatistics(forceUpdate: Boolean) = dto.statistics ?: client.profileRepository.getProfileStatistics(id)
        .orThrow { EntityNotFoundException() }

    override suspend fun withStatistics(forceUpdate: Boolean) = ProfileWithStatisticsImpl(client, dto, retrieveStatistics())

    override suspend fun retrieveGames(selector: FinishedGameSelector) = client.finishedGameRepository.getProfileHistory(id, selector)
        .orThrow { EntityNotFoundException() }
}

internal class ProfileWithStatisticsImpl(
    private val client: HmdApiClient,
    private val dto: ProfileDto,
    override val statistics: ProfileStatistics,
) : HmdProfileWithStatistics, HmdProfile by ProfileImpl(client, dto) {
    override suspend fun retrieveStatistics(forceUpdate: Boolean): ProfileStatistics {
        if (!forceUpdate) return statistics

        return client.profileRepository
            .getProfileStatistics(id)
            .orThrow { EntityNotFoundException() }
    }
}
