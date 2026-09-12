package de.mineking.hexo.game.model.leaderboard

interface LeaderboardRepository {
    suspend fun getLeaderboard(): Leaderboard
}
