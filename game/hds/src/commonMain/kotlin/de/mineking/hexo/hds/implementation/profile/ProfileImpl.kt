package de.mineking.hexo.hds.implementation.profile

import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.hds.implementation.HdsApiClient

internal class ProfileImpl(
    private val client: HdsApiClient,
    private val dto: ProfileDto,
) : Profile {
    override val id = dto.id
    override val url = "${client.host}/profile/${id.value}"
    override val displayName = dto.username
    override val image = dto.image
    override val registeredAt = dto.registeredAt

    override suspend fun retrieveStatistics(forceUpdate: Boolean) = client.profileRepository.getProfileStatistics(id) ?: error("profile not found")
    override suspend fun withStatistics(forceUpdate: Boolean) = ProfileWithStatisticsImpl(client, dto, retrieveStatistics())

    override suspend fun retrieveGames(page: Int, pageSize: Int, rated: Boolean?): List<FinishedGame> {
        TODO("FIX BEFORE COMMIT")
    }
}

internal class ProfileWithStatisticsImpl(
    private val client: HdsApiClient,
    private val dto: ProfileDto,
    override val statistics: ProfileStatistics,
) : ProfileWithStatistics, Profile by ProfileImpl(client, dto) {
    override suspend fun retrieveStatistics(forceUpdate: Boolean): ProfileStatistics {
        if (!forceUpdate) return statistics
        return client.profileRepository.getProfileStatistics(id) ?: error("profile not found")
    }
}
