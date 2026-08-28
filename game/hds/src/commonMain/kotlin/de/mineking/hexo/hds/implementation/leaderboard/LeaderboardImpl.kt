package de.mineking.hexo.hds.implementation.leaderboard

import de.mineking.hexo.game.model.EntityNotFoundException
import de.mineking.hexo.game.model.leaderboard.Leaderboard
import de.mineking.hexo.game.model.leaderboard.LeaderboardEntry
import de.mineking.hexo.game.model.profile.ProfileGameStatistics
import de.mineking.hexo.game.model.profile.ProfileRating
import de.mineking.hexo.game.model.profile.getProfileById
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.types.orThrow

internal class LeaderboardImpl(
    private val client: HdsApiClient,
    private val dto: LeaderboardDto,
) : Leaderboard {
    override val generatedAt = dto.generatedAt
    override val nextRefreshAt = dto.nextRefreshAt

    override val players = dto.players.mapIndexed { index, entry ->
        LeaderboardEntryImpl(client, entry, rank = index + 1)
    }
}

internal class LeaderboardEntryImpl(
    private val client: HdsApiClient,
    private val dto: LeaderboardEntryDto,
    rank: Int,
) : LeaderboardEntry {
    override val profileId = dto.profileId
    override val displayName = dto.displayName
    override val image = dto.image
    override val rating = ProfileRating(rank, dto.elo)
    override val totalGames = ProfileGameStatistics(dto.gamesPlayed, dto.gamesWon)

    override suspend fun retrieveProfile() = client.profileRepository
        .getProfileById(profileId)
        .orThrow { EntityNotFoundException() }
}
