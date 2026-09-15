package de.mineking.hexo.hds.implementation.profile

import de.mineking.hexo.game.model.EntityNotFoundException
import de.mineking.hexo.game.model.game.rated
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.game.model.urlOf
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.types.Selector
import de.mineking.hexo.utils.types.orThrow
import de.mineking.hexo.utils.types.page

internal class ProfileImpl(
    private val client: HdsApiClient,
    private val dto: ProfileDto,
) : Profile {
    override val id = dto.id
    override val url = client.profileRepository.urlOf(id)
    override val displayName = dto.username
    override val image = dto.image
    override val registeredAt = dto.registeredAt

    override suspend fun retrieveStatistics(forceUpdate: Boolean) = client.profileRepository.getProfileStatistics(id)
        .orThrow { EntityNotFoundException() }

    override suspend fun withStatistics(forceUpdate: Boolean) = ProfileWithStatisticsImpl(client, dto, retrieveStatistics())

    override suspend fun retrieveGames(page: Int, pageSize: Int, rated: Boolean?) =
        client.finishedGameRepository.getProfileHistory(
            id,
            Selector
                .page(page, pageSize)
                .rated(rated),
        ).orThrow { EntityNotFoundException() }
}

internal class ProfileWithStatisticsImpl(
    private val client: HdsApiClient,
    private val dto: ProfileDto,
    override val statistics: ProfileStatistics,
) : ProfileWithStatistics, Profile by ProfileImpl(client, dto) {
    override suspend fun retrieveStatistics(forceUpdate: Boolean): ProfileStatistics {
        if (!forceUpdate) return statistics

        return client.profileRepository
            .getProfileStatistics(id)
            .orThrow { EntityNotFoundException() }
    }
}
