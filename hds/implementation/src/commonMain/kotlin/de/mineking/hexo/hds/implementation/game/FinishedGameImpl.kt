package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.model.game.FinishedGame
import de.mineking.hexo.hds.model.game.FinishedGameMove
import de.mineking.hexo.hds.model.game.FinishedGamePlayer
import de.mineking.hexo.hds.model.game.GameId
import de.mineking.hexo.hds.model.game.GameOptions
import de.mineking.hexo.hds.model.game.GameResult
import de.mineking.hexo.hds.model.game.Player
import de.mineking.hexo.hds.model.game.PlayerId
import de.mineking.hexo.hds.model.game.TournamentMatchSnapshot
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.profile.ProfileRepository
import de.mineking.hexo.hds.model.tournament.TournamentBracket
import de.mineking.hexo.hds.model.tournament.TournamentId
import de.mineking.hexo.hds.model.tournament.TournamentMatchId
import de.mineking.hexo.hds.model.tournament.TournamentRepository
import kotlin.time.Instant

internal class FinishedGameImpl(
    override val id: GameId,
    override val startedAt: Instant,
    override val url: String,
    override val result: GameResult,
    override val options: GameOptions,
    override val tournamentInfo: TournamentMatchSnapshot?,
    override val moves: List<FinishedGameMove>,
    override val moveCount: Int,
    override val players: List<FinishedGamePlayer>,
) : FinishedGame {
    companion object {
        private fun FinishedGameDto.createPlayerList(repository: ProfileRepository) = players.map { data ->
            val color = playerTiles[data.playerId]?.color ?: error("Player tile for ${data.playerId} not defined")
            val owner = when {
                color.red > 200 -> CellOwner.X
                color.blue > 200 -> CellOwner.O
                else -> error("Unrecognized color '${color.format()}'")
            }

            FinishedGamePlayerImpl(
                repository = repository,
                playerId = data.playerId,
                profileId = data.profileId.takeIf { it.value != data.playerId.value },
                displayName = data.displayName,
                elo = data.elo,
                eloChange = data.eloChange,
                color = owner,
                tournamentMatchWins = tournament?.let {
                    when (data.profileId) {
                        it.leftProfileId -> it.leftWins
                        it.rightProfileId -> it.rightWins
                        else -> error("Inconsistent tournament snapshot")
                    }
                },
            )
        }.sortedBy { player -> moves.indexOfFirst { it.playerId == player.playerId } }

        internal fun of(
            client: HdsApiClient,
            dto: FinishedGameDto,
        ): FinishedGame {
            val players = dto.createPlayerList(client.profileRepository)
            val playersById = players.associateBy { it.playerId }

            return FinishedGameImpl(
                id = dto.id,
                startedAt = dto.startedAt,
                url = "${client.host}/games/${dto.id.value}",
                result = GameResult(playersById[dto.result.winningPlayerId], dto.result.duration, dto.result.reason),
                options = dto.options,
                tournamentInfo = dto.tournament?.let { TournamentMatchSnapshotImpl.of(it, client) },
                moves = dto.moves.map {
                    FinishedGameMove(
                        coordinate = CellCoordinate(it.q, it.r),
                        player = playersById[it.playerId]!!,
                        it.timestamp,
                    )
                },
                moveCount = dto.moveCount,
                players = players,
            )
        }
    }
}

internal open class PlayerImpl(
    private val repository: ProfileRepository,
    override val playerId: PlayerId,
    override val profileId: ProfileId?,
    override val displayName: String,
    override val elo: Int,
    override val color: CellOwner,
    override val tournamentMatchWins: Int?,
) : Player {
    override suspend fun fetchProfile() = profileId?.let { repository.getProfile(it) }
}

private class FinishedGamePlayerImpl(
    repository: ProfileRepository,
    override val playerId: PlayerId,
    override val profileId: ProfileId?,
    override val displayName: String,
    override val elo: Int,
    override val eloChange: Int?,
    override val color: CellOwner,
    override val tournamentMatchWins: Int?,
) : FinishedGamePlayer, PlayerImpl(
    repository = repository,
    playerId = playerId,
    profileId = profileId,
    displayName = displayName,
    elo = elo,
    color = color,
    tournamentMatchWins = tournamentMatchWins,
)

internal class TournamentMatchSnapshotImpl(
    private val repository: TournamentRepository,
    override val tournamentId: TournamentId,
    override val tournamentUrl: String,
    override val tournamentName: String,
    override val matchId: TournamentMatchId,
    override val bracket: TournamentBracket,
    override val round: Int,
    override val order: Int,
    override val bestOf: Int,
    override val currentGameNumber: Int,
) : TournamentMatchSnapshot {
    override suspend fun retrieveTournament() = repository.getTournament(tournamentId)
    override fun observeTournament() = repository.observeTournament(tournamentId)

    companion object {
        internal fun of(
            dto: TournamentMatchSnapshotDto,
            client: HdsApiClient,
        ) = TournamentMatchSnapshotImpl(
            repository = client.tournamentRepository,
            tournamentId = dto.tournamentId,
            tournamentUrl = "${client.host}/tournaments/${dto.tournamentId.value}",
            tournamentName = dto.tournamentName,
            matchId = dto.matchId,
            bracket = dto.bracket,
            round = dto.round,
            order = dto.order,
            bestOf = dto.bestOf,
            currentGameNumber = dto.currentGameNumber,
        )
    }
}
