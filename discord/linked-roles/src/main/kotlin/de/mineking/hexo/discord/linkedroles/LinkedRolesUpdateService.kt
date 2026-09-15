package de.mineking.hexo.discord.linkedroles

import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.discord.oauth2.DiscordOAuth2Client
import de.mineking.hexo.discord.oauth2.LinkedRoleMetadataKey
import de.mineking.hexo.discord.oauth2.LinkedRoleMetadataType
import de.mineking.hexo.discord.oauth2.OAuth2TokenRepository
import de.mineking.hexo.discord.oauth2.OAuth2Tokens
import de.mineking.hexo.discord.oauth2.bindValue
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.profile.ProfileWithStatistics
import de.mineking.hexo.game.model.profile.getProfileById
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.getDiscordProfile
import de.mineking.hexo.utils.coroutines.KeyedSemaphore
import de.mineking.hexo.utils.coroutines.awaitBoth
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.flatMap
import de.mineking.hexo.utils.types.orElse
import de.mineking.hexo.utils.types.successIfNotNull
import io.github.oshai.kotlinlogging.KotlinLogging

internal object RankKey : LinkedRoleMetadataKey<Int>("rank", LinkedRoleMetadataType.IntegerLessThanOrEqual)
internal object EloKey : LinkedRoleMetadataKey<Int>("elo", LinkedRoleMetadataType.IntegerGreaterThanOrEqual)

private val logger = KotlinLogging.logger {}

class LinkedRolesUpdateService(
    private val accountLinkRepository: AccountLinkRepository,
    private val discordUserAuthenticationRepository: OAuth2TokenRepository,
    private val discordOAuth2Client: DiscordOAuth2Client,
    private val profileRepository: ProfileRepository,
) {
    private val updateSemaphore = KeyedSemaphore<DiscordUserId>()

    suspend fun updateLinkedRolesData(tokens: OAuth2Tokens) = updateLinkedRolesData(tokens.id) { Result.Success(tokens) }
    suspend fun updateLinkedRolesData(discordId: DiscordUserId) = updateLinkedRolesData(discordId) {
        discordUserAuthenticationRepository.getUserTokens(discordId)
    }

    private suspend fun updateLinkedRolesData(discordId: DiscordUserId, tokens: suspend () -> Result<OAuth2Tokens, *>) {
        val (profile, tokens) = awaitBoth(
            first = {
                accountLinkRepository.getHexoProfile(discordId)
                    .successIfNotNull()
                    .flatMap { profileRepository.getProfileById(it) }
            },
            second = { tokens() },
        ).orElse { return }

        updateLinkedRolesData(profile, tokens)
    }

    suspend fun updateLinkedRolesData(profile: ProfileWithStatistics) = updateLinkedRolesData(profile.id) { Result.Success(profile) }
    suspend fun updateLinkedRolesData(profileId: ProfileId) = updateLinkedRolesData(profileId) {
        profileRepository.getProfileById(profileId)
    }

    private suspend fun updateLinkedRolesData(profileId: ProfileId, profile: suspend () -> Result<ProfileWithStatistics, *>) {
        val (profile, tokens) = awaitBoth(
            first = { profile() },
            second = {
                accountLinkRepository.getDiscordProfile(profileId)
                    .successIfNotNull()
                    .flatMap { discordUserAuthenticationRepository.getUserTokens(it) }
            },
        ).orElse { return }

        updateLinkedRolesData(profile, tokens)
    }

    private suspend fun updateLinkedRolesData(profile: ProfileWithStatistics, tokens: OAuth2Tokens) = updateSemaphore.withPermit(tokens.id) {
        logger.info { "Updating linked role data for (discord=${tokens.id.value},hexo=${profile.id.value})" }

        @Suppress("TooGenericExceptionCaught")
        try {
            discordOAuth2Client.updateLinkedRoleData(
                user = tokens,
                values = arrayOf(
                    RankKey.bindValue(profile.statistics.rating.worldRank),
                    EloKey.bindValue(profile.statistics.rating.elo),
                ),
            )
        } catch (e: Exception) {
            logger.error(e) { "Exception while updating linked role data update" }
        }
    }
}
