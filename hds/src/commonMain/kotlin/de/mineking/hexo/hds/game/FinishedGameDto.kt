package de.mineking.hexo.hds.game

import de.mineking.hexo.hds.profile.ProfileId
import de.mineking.hexo.hds.tournament.TournamentBracket
import de.mineking.hexo.hds.tournament.TournamentId
import de.mineking.hexo.hds.tournament.TournamentMatchId
import de.mineking.hexo.hds.utils.Color
import de.mineking.hexo.hds.utils.Duration
import de.mineking.hexo.hds.utils.Instant
import de.mineking.hexo.hds.utils.TimeControl
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
data class TournamentMatchSnapshotDto(
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
enum class GameFinishReason {
    @SerialName("six-in-a-row") SixInARow,
    @SerialName("timeout") Timeout,
    @SerialName("surrender") Surrender,
    @SerialName("disconnect") Disconnect,
    @SerialName("draw-agreement") DrawAgreement,
    @SerialName("terminated") Terminated,
}

@Serializable
internal data class GameResultDto(
    val winningPlayerId: PlayerId?,
    @SerialName("durationMs") val duration: Duration,
    val reason: GameFinishReason,
)

@Serializable
enum class GameVisibility {
    @SerialName("public") Public,
    @SerialName("private") Private,
}

@Serializable
data class GameOptions(
    val rated: Boolean,
    val visibility: GameVisibility,
    val timeControl: TimeControl,
)
