package de.mineking.hexo.hds.model.leaderboard

interface LeaderboardRepository {
    suspend fun getLeaderboard(): Leaderboard
}
