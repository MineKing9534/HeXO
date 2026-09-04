@file:OptIn(ExperimentalSerializationApi::class)

package de.mineking.hexo.hds.implementation.profile

import de.mineking.hexo.game.model.profile.ProfileGameStatistics
import de.mineking.hexo.game.model.profile.ProfileGameStatisticsWithStreak
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileRating
import de.mineking.hexo.game.model.profile.ProfileStatistics
import de.mineking.hexo.hds.implementation.Instant
import de.mineking.hexo.hds.implementation.utils.JsonUnwrapSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object ProfileSerializer : JsonUnwrapSerializer<ProfileDto>(ProfileDto.generatedSerializer(), "user")

@KeepGeneratedSerializer
@Serializable(ProfileSerializer::class)
internal data class ProfileDto(
    val id: ProfileId,
    val username: String,
    val image: String?,
    val registeredAt: Instant,
    val lastActiveAt: Instant,
)

@Serializable
internal data class ProfileStatisticsDto(
    val elo: Int,
    val worldRank: Int?,
    val eloHistory: ProfileEloHistoryDto,
    override val totalGames: ProfileGameStatisticsDto,
    @SerialName("rankedGames") override val ratedGames: ProfileGameStatisticsWithStreakDto,
) : ProfileStatistics {
    override val rating = ProfileRating(worldRank, elo)
    override val highestRating = ProfileRating(worldRank, eloHistory.highestPoint?.elo ?: elo)
}

@Serializable
internal data class ProfileEloHistoryDto(
    val points: List<Point>,
) {
    val highestPoint = points.maxByOrNull { it.elo }

    @Serializable
    internal data class Point(
        val timestamp: Instant,
        val elo: Int,
    )
}

@Serializable
internal data class ProfileGameStatisticsDto(
    @SerialName("played") override val total: Int,
    override val won: Int,
) : ProfileGameStatistics

@Serializable
internal data class ProfileGameStatisticsWithStreakDto(
    @SerialName("played") override val total: Int,
    override val won: Int,
    override val currentWinStreak: Int,
    override val longestWinStreak: Int,
) : ProfileGameStatisticsWithStreak
