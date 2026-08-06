package de.mineking.hexo.hds.model.profile

import de.mineking.hexo.hds.model.Duration
import de.mineking.hexo.hds.model.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ProfileId(val value: String)

interface Profile {
    val id: ProfileId
    val url: String
    val displayName: String
    val image: String?
    val registeredAt: Instant
    val lastActiveAt: Instant

    suspend fun retrieveStatistics(forceUpdate: Boolean = false): ProfileStatistics?
}

interface RichProfile : Profile {
    val statistics: ProfileStatistics
}

@Serializable
data class ProfileStatistics(
    @SerialName("longestGamePlayedMs") val longestGameByDuration: Duration,
    val longestGameByMoves: Int,
    val totalMovesMade: Int,
    val elo: Int,
    val worldRank: Int?,
    val totalGames: TotalGames,
    val rankedGames: TotalRankedGames,
    val eloHistory: ProfileEloHistory,
) {
    sealed interface GameStatistics {
        val played: Int
        val won: Int

        val winRate get() = won.toDouble() / played
    }

    @Serializable
    data class TotalGames(
        override val played: Int,
        override val won: Int,
    ) : GameStatistics

    @Serializable
    data class TotalRankedGames(
        override val played: Int,
        override val won: Int,
        val currentWinStreak: Int,
        val longestWinStreak: Int,
    ) : GameStatistics
}

@Serializable
data class ProfileEloHistory(
    @SerialName("bucketSizeMs") val bucketSize: Duration,
    val points: List<ProfileEloHistoryPoint>,
) {
    val highestPoint = points.maxByOrNull { it.elo }
}

@Serializable
data class ProfileEloHistoryPoint(
    val timestamp: Instant,
    val elo: Int,
)
