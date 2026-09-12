package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.game.model.game.GameFinishReason
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.game.GameOptions
import de.mineking.hexo.game.model.game.GameResult
import de.mineking.hexo.game.model.game.GameVisibility
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.game.PlayerId
import de.mineking.hexo.game.model.game.TournamentMatchSnapshot
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.tournament.TournamentId
import de.mineking.hexo.game.model.tournament.TournamentInfo
import de.mineking.hexo.game.model.tournament.TournamentMatchId
import de.mineking.hexo.game.model.tournament.TournamentMatchInfo
import de.mineking.hexo.game.model.tournament.TournamentReference
import de.mineking.hexo.hds.implementation.Duration
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.Instant
import de.mineking.hexo.hds.implementation.TimeControl
import de.mineking.hexo.hds.implementation.tournament.TournamentBracketDto
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
    tournament = TournamentReference(
        repository = client.tournamentRepository,
        info = TournamentInfo(
            id = tournamentId,
            url = "${client.host}/tournaments/${tournamentId.value}",
            name = tournamentName,
        ),
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
internal data class PlayerTile(val colorIndex: Int) {
    val color get() = CellOwner.entries[colorIndex]
}

internal interface AbstractPlayerDto {
    val playerId: PlayerId
    val profileId: ProfileId?
    val displayName: String
    val elo: Int
}

@Serializable
internal data class PlayerDto(
    override val playerId: PlayerId,
    override val profileId: ProfileId,
    override val displayName: String,
    override val elo: Int,
    val eloChange: Int?,
) : AbstractPlayerDto

@Serializable
internal data class GameResultDto(
    val winningPlayerId: PlayerId?,
    val abortedByPlayerId: PlayerId? = null,
    @SerialName("durationMs") val duration: Duration,
    val reason: GameFinishReasonDto,
) {
    fun toModel(players: List<Player>) = GameResult(
        winner = winningPlayerId?.let { players.single { it.id == winningPlayerId } },
        duration = duration,
        reason = reason.toModal(this, players),
    )
}

@Serializable
internal enum class GameFinishReasonDto {
    @SerialName("six-in-a-row") SixInARow {
        override fun toModal(result: GameResultDto, players: List<Player>) = GameFinishReason.Regular(6)
    },
    @SerialName("timeout") Timeout {
        override fun toModal(result: GameResultDto, players: List<Player>) = GameFinishReason.Timeout
    },
    @SerialName("surrender") Surrender {
        override fun toModal(result: GameResultDto, players: List<Player>) = GameFinishReason.Surrender(
            surrenderingPlayer = players.single { it.id != result.winningPlayerId },
        )
    },
    @SerialName("disconnect") Disconnect {
        override fun toModal(result: GameResultDto, players: List<Player>) = GameFinishReason.Disconnect(
            disconnectedPlayer = players.single { it.id != result.winningPlayerId },
        )
    },
    @SerialName("draw-agreement") DrawAgreement {
        override fun toModal(result: GameResultDto, players: List<Player>) = GameFinishReason.DrawAgreement
    },
    @SerialName("terminated") Terminated {
        override fun toModal(result: GameResultDto, players: List<Player>) = GameFinishReason.Terminated
    },
    @SerialName("aborted") Aborted {
        override fun toModal(result: GameResultDto, players: List<Player>) = GameFinishReason.Aborted(
            abortingPlayer = players.single { it.id == result.abortedByPlayerId },
        )
    },
    ;

    abstract fun toModal(result: GameResultDto, players: List<Player>): GameFinishReason
}
