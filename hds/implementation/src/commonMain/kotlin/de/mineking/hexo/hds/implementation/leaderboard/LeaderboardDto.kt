package de.mineking.hexo.hds.implementation.leaderboard

import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.profile.ProfileId
import kotlinx.serialization.Serializable

@Serializable
internal data class LeaderboardDto(
    val generatedAt: Instant,
    val nextRefreshAt: Instant,
    val players: List<LeaderboardEntryDto>,
)

@Serializable
internal data class LeaderboardEntryDto(
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val elo: Int,
    val gamesPlayed: Int,
    val gamesWon: Int,
)
