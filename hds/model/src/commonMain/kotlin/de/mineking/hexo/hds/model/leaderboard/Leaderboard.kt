package de.mineking.hexo.hds.model.leaderboard

import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.profile.Profile
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.profile.ProfileStatistics

interface Leaderboard {
    val generatedAt: Instant
    val nextRefreshAt: Instant
    val players: List<LeaderboardEntry>
}

interface LeaderboardEntry {
    val profileId: ProfileId
    val displayName: String
    val image: String?
    val elo: Int
    val totalGames: ProfileStatistics.TotalGames

    suspend fun retrieveProfile(): Profile?
}
