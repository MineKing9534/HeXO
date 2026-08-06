package de.mineking.hexo.hds.implementation.leaderboard

import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.leaderboard.Leaderboard
import de.mineking.hexo.hds.model.leaderboard.LeaderboardEntry
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.profile.ProfileRepository
import de.mineking.hexo.hds.model.profile.ProfileStatistics

internal class LeaderboardImpl(
    override val generatedAt: Instant,
    override val nextRefreshAt: Instant,
    override val players: List<LeaderboardEntry>,
) : Leaderboard {
    companion object {
        internal fun of(client: HdsApiClient, dto: LeaderboardDto): Leaderboard {
            return LeaderboardImpl(
                generatedAt = dto.generatedAt,
                nextRefreshAt = dto.nextRefreshAt,
                players = dto.players.map {
                    LeaderboardEntryImpl(
                        repository = client.profileRepository,
                        profileId = it.profileId,
                        displayName = it.displayName,
                        image = it.image,
                        elo = it.elo,
                        totalGames = ProfileStatistics.TotalGames(
                            played = it.gamesPlayed,
                            won = it.gamesWon,
                        ),
                    )
                },
            )
        }
    }
}

private class LeaderboardEntryImpl(
    private val repository: ProfileRepository,
    override val profileId: ProfileId,
    override val displayName: String,
    override val image: String?,
    override val elo: Int,
    override val totalGames: ProfileStatistics.TotalGames,
) : LeaderboardEntry {
    override suspend fun retrieveProfile() = repository.getProfile(profileId)
}
