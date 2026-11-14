package de.mineking.hexo.hds.implementation.profile

import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.game.model.game.FinishedGameSelector
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.types.EntityNotFoundException
import de.mineking.hexo.utils.types.orThrow
import de.mineking.hexo.utils.types.urlOf

internal class ProfileImpl(
    private val client: HdsApiClient,
    private val dto: ProfileDto,
) : Profile {
    companion object {
        private val DISCORD_AVATAR_PATTERN = """https://cdn\.discordapp\.com/avatars/(\d+)/.*\.(?:png|jpg|webp|gif)(?:\?size=\d+)?""".toRegex()
    }

    override val id = dto.id
    override val url = client.profileRepository.urlOf(id)
    override val discordId = dto.image
        ?.let { DISCORD_AVATAR_PATTERN.matchEntire(it) }
        ?.let { DiscordUserId(it.groupValues[1].toLong()) }
    override val displayName = dto.username
    override val image = dto.image
    override val registeredAt = dto.registeredAt

    override suspend fun retrieveStatistics(forceUpdate: Boolean) = client.profileRepository.getProfileStatistics(id)
        .orThrow { EntityNotFoundException() }

    override suspend fun withStatistics(forceUpdate: Boolean) = ProfileWithStatisticsImpl(client, dto, retrieveStatistics())

    override suspend fun retrieveGames(selector: FinishedGameSelector) = client.finishedGameRepository.getProfileHistory(id, selector)
        .orThrow { EntityNotFoundException() }
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
