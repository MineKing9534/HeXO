package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.game.model.game.GameFinishReason
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.game.GameOptions
import de.mineking.hexo.game.model.game.GameVisibility
import de.mineking.hexo.game.model.game.PlayerId
import de.mineking.hexo.game.model.game.TournamentMatchSnapshot
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.tournament.TournamentId
import de.mineking.hexo.game.model.tournament.TournamentInfo
import de.mineking.hexo.game.model.tournament.TournamentMatchId
import de.mineking.hexo.game.model.tournament.TournamentMatchInfo
import de.mineking.hexo.hds.implementation.Duration
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.Instant
import de.mineking.hexo.hds.implementation.TimeControl
import de.mineking.hexo.hds.implementation.tournament.TournamentBracketDto
import de.mineking.hexo.hds.implementation.utils.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FinishedGameDto(
    val id: GameId,
    val startedAt: Instant,
    val players: List<PlayerDto>,
    val playerTiles: Map<PlayerId, PlayerTile>,
    @SerialName("gameResult") val result: GameResultDto,
    @SerialName("gameOptions") val options: GameOptionsDto,
    val tournament: TournamentMatchSnapshotDto?,
    val moves: List<MoveDto> = emptyList(),
    val moveCount: Int,
)

@Serializable
internal data class GameOptionsDto(
    override val rated: Boolean,
    override val visibility: GameVisibility,
    override val timeControl: TimeControl,
) : GameOptions

@Serializable
internal data class TournamentMatchSnapshotDto(
    val tournamentId: TournamentId,
    val tournamentName: String,
    val matchId: TournamentMatchId,
    val bracket: TournamentBracketDto,
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

internal fun TournamentMatchSnapshotDto.toModel(client: HdsApiClient) = TournamentMatchSnapshot(
    repository = client.tournamentRepository,
    tournamentInfo = TournamentInfo(
        id = tournamentId,
        url = "${client.host}/tournaments/${tournamentId.value}",
        name = tournamentName,
    ),
    matchInfo = TournamentMatchInfo(
        id = matchId,
        bracket = bracket.model,
        round = round,
        order = order,
        bestOf = bestOf,
        currentGameNumber = currentGameNumber,
    ),
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
    val reason: GameFinishReasonDto,
)

@Serializable
enum class GameFinishReasonDto(val model: GameFinishReason) {
    @SerialName("six-in-a-row") SixInARow(GameFinishReason.Regular(6)),
    @SerialName("timeout") Timeout(GameFinishReason.Timeout),
    @SerialName("surrender") Surrender(GameFinishReason.Surrender),
    @SerialName("disconnect") Disconnect(GameFinishReason.Disconnect),
    @SerialName("draw-agreement") DrawAgreement(GameFinishReason.DrawAgreement),
    @SerialName("terminated") Terminated(GameFinishReason.Terminated),
}
