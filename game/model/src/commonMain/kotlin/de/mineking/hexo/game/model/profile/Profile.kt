package de.mineking.hexo.game.model.profile

import de.mineking.hexo.game.model.game.FinishedGame
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
@Serializable
value class ProfileId(val value: String)

interface Profile {
    val id: ProfileId
    val url: String
    val displayName: String
    val image: String?
    val registeredAt: Instant

    suspend fun retrieveStatistics(forceUpdate: Boolean = false): ProfileStatistics
    suspend fun withStatistics(forceUpdate: Boolean = false): ProfileWithStatistics

    suspend fun retrieveGames(page: Int, pageSize: Int, rated: Boolean? = null): List<FinishedGame>
}

interface ProfileWithStatistics : Profile {
    val statistics: ProfileStatistics
}

interface ProfileStatistics {
    val rating: ProfileRating
    val highestRating: ProfileRating

    val totalGames: ProfileGameStatistics
    val ratedGames: ProfileGameStatisticsWithStreak
}

interface ProfileGameStatistics {
    val total: Int
    val won: Int

    val winRate get() = won.toDouble() / total
}

interface ProfileGameStatisticsWithStreak : ProfileGameStatistics {
    val currentWinStreak: Int
    val longestWinStreak: Int
}

@Suppress("FunctionNaming")
fun ProfileGameStatistics(
    total: Int,
    won: Int,
) = object : ProfileGameStatistics {
    override val total = total
    override val won = won
}

@Suppress("FunctionNaming")
fun ProfileGameStatisticsWithStreak(
    total: Int,
    won: Int,
    currentWinStreak: Int,
    longestWinStreak: Int,
): ProfileGameStatisticsWithStreak = object : ProfileGameStatisticsWithStreak, ProfileGameStatistics by ProfileGameStatistics(total, won) {
    override val currentWinStreak = currentWinStreak
    override val longestWinStreak = longestWinStreak
}

interface ProfileRating {
    val worldRank: Int?
    val elo: Int
}

@Suppress("FunctionNaming")
fun ProfileRating(worldRank: Int?, rating: Int) = object : ProfileRating {
    override val worldRank = worldRank
    override val elo = rating
}
