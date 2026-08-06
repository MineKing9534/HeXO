package de.mineking.hexo.hds.implementation.tournament

import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.TimeControl
import de.mineking.hexo.hds.model.game.GameId
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.session.SessionId
import de.mineking.hexo.hds.model.tournament.TournamentBracket
import de.mineking.hexo.hds.model.tournament.TournamentId
import de.mineking.hexo.hds.model.tournament.TournamentMatchId
import de.mineking.hexo.hds.model.tournament.TournamentMatchResultType
import de.mineking.hexo.hds.model.tournament.TournamentMatchState
import de.mineking.hexo.hds.model.tournament.TournamentStanding
import de.mineking.hexo.hds.model.tournament.TournamentStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class TournamentFormatType {
    @SerialName("single-elimination") SingleElimination,
    @SerialName("double-elimination") DoubleElimination,
    @SerialName("swiss") Swiss,
}

@Serializable
internal data class TournamentDto(
    val id: TournamentId,
    val name: String,
    val description: String?,
    val format: TournamentFormatType,
    val status: TournamentStatus,
    val scheduledStartAt: Instant,
    val checkInOpensAt: Instant,
    val checkInClosesAt: Instant,
    val maxPlayers: Int,
    val registeredCount: Int,
    val checkedInCount: Int,
    val timeControl: TimeControl,
    val participants: List<TournamentParticipantDto>,
    val standings: List<TournamentStanding>,
    val matches: List<TournamentMatchDto>,
    val swissRoundCount: Int?,
)

@Serializable
internal data class TournamentParticipantDto(
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val registeredAt: Instant,
    val seed: Int?,
)

@Serializable
internal data class TournamentMatchSlotDto(
    val profileId: ProfileId?,
    val seed: Int?,
    val isBye: Boolean,
)

@Serializable
internal data class TournamentMatchDto(
    val id: TournamentMatchId,
    val bracket: TournamentBracket,
    val round: Int,
    val order: Int,
    val state: TournamentMatchState,
    val bestOf: Int,
    val currentGameNumber: Int,
    val leftWins: Int,
    val rightWins: Int,
    val winnerProfileId: ProfileId?,
    val loserProfileId: ProfileId?,
    val resultType: TournamentMatchResultType?,
    val waitingForPlayers: Boolean,
    val startedAt: Instant?,
    val resolvedAt: Instant?,
    val slots: List<TournamentMatchSlotDto>,
    val gameIds: List<GameId>,
    val sessionId: SessionId?,
)
