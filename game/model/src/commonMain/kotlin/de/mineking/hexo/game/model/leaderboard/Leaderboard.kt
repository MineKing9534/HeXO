package de.mineking.hexo.game.model.leaderboard

import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileGameStatistics
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileRating
import kotlin.time.Instant

interface Leaderboard {
    val generatedAt: Instant
    val nextRefreshAt: Instant

    val players: List<LeaderboardEntry>
}

interface LeaderboardEntry {
    val profileId: ProfileId
    val displayName: String
    val image: String?

    val rating: ProfileRating
    val totalGames: ProfileGameStatistics

    suspend fun retrieveProfile(): Profile?
}
