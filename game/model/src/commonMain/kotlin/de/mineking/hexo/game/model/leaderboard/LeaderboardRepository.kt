package de.mineking.hexo.game.model.leaderboard

import kotlin.time.Instant

interface LeaderboardRepository {
    suspend fun getLeaderboard(): Leaderboard

    companion object Empty : LeaderboardRepository {
        private val leaderboard = object : Leaderboard {
            override val generatedAt = Instant.DISTANT_PAST
            override val nextRefreshAt = Instant.DISTANT_FUTURE

            override val players = emptyList<LeaderboardEntry>()
        }

        override suspend fun getLeaderboard() = leaderboard
    }
}
