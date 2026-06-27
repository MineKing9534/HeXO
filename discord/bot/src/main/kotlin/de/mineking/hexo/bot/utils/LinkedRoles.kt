package de.mineking.hexo.bot.utils

import de.mineking.discord.DiscordToolKit
import de.mineking.discord.localization.LocalizationFile
import de.mineking.hexo.hds.createCoroutineScope
import de.mineking.hexo.hds.game.FinishedGameRepository
import de.mineking.hexo.hds.game.GameId
import de.mineking.hexo.hds.profile.ProfileId
import de.mineking.hexo.hds.profile.ProfileRepository
import de.mineking.hexo.hds.profile.RichProfile
import de.mineking.hexo.hds.utils.awaitBothOrNull
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.getDiscordProfile
import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import de.mineking.hexo.link.oauth2.LinkedRoleMetadataKey
import de.mineking.hexo.link.oauth2.LinkedRoleMetadataType
import de.mineking.hexo.link.oauth2.OAuth2Tokens
import de.mineking.hexo.link.oauth2.bindValue
import de.mineking.hexo.link.oauth2.updateLinkedRoleMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration.Companion.minutes

private val RankKey = LinkedRoleMetadataKey("rank", LinkedRoleMetadataType.IntegerLessThanOrEqual)
private val EloKey = LinkedRoleMetadataKey("elo", LinkedRoleMetadataType.IntegerGreaterThanOrEqual)

private val logger = KotlinLogging.logger {}

class LinkedRolesUpdateService(
    private val accountLinkRepository: AccountLinkRepository,
    private val discordUserAuthenticationRepository: DiscordUserAuthenticationRepository,
    private val finishedGameRepository: FinishedGameRepository,
    private val profileRepository: ProfileRepository,
) {
    private val coroutineScope = createCoroutineScope(logger)
    private val semaphore = Semaphore(10)

    init {
        coroutineScope.launch {
            var lastSeenGame = GameId("")
            while (true) {
                @Suppress("TooGenericExceptionCaught")
                try {
                    val finishedGames = finishedGameRepository.getFinishedGames(1, 20, rated = true)
                        .takeWhile { it.id != lastSeenGame }

                    if (finishedGames.isNotEmpty()) {
                        finishedGames
                            .flatMapTo(mutableSetOf()) { it.players }
                            .mapNotNull { it.profileId }
                            .toSet()
                            .forEach { scheduleLinkedRoleDataUpdate(it) }

                        lastSeenGame = finishedGames.first().id
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Exception while automatic linked roles update" }
                }

                delay(1.minutes)
            }
        }
    }

    fun scheduleLinkedRoleDataUpdate(tokens: OAuth2Tokens) {
        coroutineScope.launch {
            semaphore.withPermit {
                val profileId = accountLinkRepository.getHexoProfile(tokens.id) ?: return@withPermit
                val profile = profileRepository.getProfile(profileId) ?: return@withPermit

                updateLinkedRoleData(profile, tokens)
            }
        }
    }

    fun scheduleLinkedRoleDataUpdate(profile: ProfileId) {
        coroutineScope.launch {
            semaphore.withPermit {
                val (profile, tokens) = awaitBothOrNull(
                    first = { profileRepository.getProfile(profile) },
                    second = {
                        val user = accountLinkRepository.getDiscordProfile(profile) ?: return@awaitBothOrNull null
                        discordUserAuthenticationRepository.getUserTokens(user)
                    },
                ) ?: return@withPermit

                updateLinkedRoleData(profile, tokens)
            }
        }
    }

    private suspend fun updateLinkedRoleData(profile: RichProfile, tokens: OAuth2Tokens) {
        logger.info { "Updating linked role data for (discord=${tokens.id.value},hexo=${profile.id.value})" }

        @Suppress("TooGenericExceptionCaught")
        try {
            discordUserAuthenticationRepository.discordOAuth2Client.updateLinkedRoleData(
                user = tokens,
                values = arrayOf(
                    RankKey.bindValue(profile.statistics.worldRank),
                    EloKey.bindValue(profile.statistics.elo),
                ),
            )
        } catch (e: Exception) {
            logger.error(e) { "Exception while updating linked role data update" }
        }
    }
}

suspend fun DiscordToolKit<*>.updateLinkedRoleMetadata() {
    logger.info { "Updating linked roles metadata..." }
    updateLinkedRoleMetadata<LinkedRolesMetadataLocalization>(
        RankKey,
        EloKey,
    )
}

interface LinkedRolesMetadataLocalization : LocalizationFile
