package de.mineking.hexo.game.implementation.protocol

import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.game.model.profile.ProfileGameStatistics
import de.mineking.hexo.game.model.profile.ProfileGameStatisticsWithStreak
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileRating
import de.mineking.hexo.game.model.profile.ProfileStatistics
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ProfileDto(
    val id: ProfileId,
    val discord: DiscordUserId,
    val displayName: String,
    val image: String?,
    val registeredAt: Instant,
    val statistics: ProfileStatisticsDto? = null,
)

@Serializable
data class ProfileStatisticsDto(
    override val rating: ProfileRatingDto,
    override val highestRating: ProfileRatingDto,
    override val totalGames: ProfileGameStatisticsDto,
    override val ratedGames: ProfileGameStatisticsWithStreakDto,
) : ProfileStatistics

@Serializable
data class ProfileRatingDto(
    override val worldRank: Int?,
    override val elo: Int,
) : ProfileRating

@Serializable
data class ProfileGameStatisticsDto(
    override val total: Int,
    override val won: Int,
) : ProfileGameStatistics

@Serializable
data class ProfileGameStatisticsWithStreakDto(
    override val total: Int,
    override val won: Int,
    override val currentWinStreak: Int,
    override val longestWinStreak: Int,
) : ProfileGameStatisticsWithStreak
