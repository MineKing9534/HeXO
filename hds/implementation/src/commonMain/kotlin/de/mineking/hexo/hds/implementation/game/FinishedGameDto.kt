package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.hds.implementation.utils.Color
import de.mineking.hexo.hds.model.Duration
import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.game.GameFinishReason
import de.mineking.hexo.hds.model.game.GameId
import de.mineking.hexo.hds.model.game.GameOptions
import de.mineking.hexo.hds.model.game.PlayerId
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.tournament.TournamentBracket
import de.mineking.hexo.hds.model.tournament.TournamentId
import de.mineking.hexo.hds.model.tournament.TournamentMatchId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FinishedGameDto(
    val id: GameId,
    val startedAt: Instant,
    val players: List<PlayerDto>,
    val playerTiles: Map<PlayerId, PlayerTile>,
    @SerialName("gameResult") val result: GameResultDto,
    @SerialName("gameOptions") val options: GameOptions,
    val tournament: TournamentMatchSnapshotDto?,
    val moves: List<MoveDto> = emptyList(),
    val moveCount: Int,
)

@Serializable
internal data class TournamentMatchSnapshotDto(
    val tournamentId: TournamentId,
    val tournamentName: String,
    val matchId: TournamentMatchId,
    val bracket: TournamentBracket,
    val round: Int,
    val order: Int,
    val bestOf: Int,
    val currentGameNumber: Int,
    val leftWins: Int,
    val rightWins: Int,
    val leftProfileId: ProfileId,
    val rightProfileId: ProfileId,
    val leftDisplayName: String,
    val rightDisplayName: String,
)

@Serializable
internal data class MoveDto(
    val playerId: PlayerId,
    @SerialName("x") val q: Int,
    @SerialName("y") val r: Int,
    val timestamp: Instant,
)

@Serializable
internal data class PlayerTile(val color: Color)

@Serializable
internal data class PlayerDto(
    val playerId: PlayerId,
    val profileId: ProfileId,
    val displayName: String,
    val elo: Int,
    val eloChange: Int?,
)

@Serializable
internal data class GameResultDto(
    val winningPlayerId: PlayerId?,
    @SerialName("durationMs") val duration: Duration,
    val reason: GameFinishReason,
)
