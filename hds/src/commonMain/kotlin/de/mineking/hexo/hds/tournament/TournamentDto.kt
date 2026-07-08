package de.mineking.hexo.hds.tournament

import de.mineking.hexo.hds.game.GameId
import de.mineking.hexo.hds.profile.ProfileId
import de.mineking.hexo.hds.session.SessionId
import de.mineking.hexo.hds.utils.Instant
import de.mineking.hexo.hds.utils.TimeControl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TournamentFormatType {
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
enum class TournamentStatus {
    @SerialName("draft") Draft,
    @SerialName("registration-open") RegistrationOpen,
    @SerialName("check-in-open") CheckInOpen,
    @SerialName("waitlist-open") WaitlistOpen,
    @SerialName("live") Live,
    @SerialName("completed") Completed,
    @SerialName("cancelled") Cancelled,
}

fun TournamentStatus.isTerminal() = this >= TournamentStatus.Completed

@Serializable
internal data class TournamentParticipantDto(
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val registeredAt: Instant,
    val seed: Int?,
)

@Serializable
data class TournamentStanding(
    val rank: Int,
    val profileId: ProfileId,
    val wins: Int,
    val losses: Int,
    val buchholz: Int,
    val sonnebornBerger: Int,
)

@Serializable
enum class TournamentMatchState {
    @SerialName("pending") Pending,
    @SerialName("ready") Ready,
    @SerialName("in-progress") InProgress,
    @SerialName("completed") Completed,
}

@Serializable
enum class TournamentMatchResultType {
    @SerialName("played") Played,
    @SerialName("bye") Byte,
    @SerialName("walkover") Walkover,
}

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

@Serializable
enum class TournamentBracket {
    @SerialName("winners") Winners,
    @SerialName("losers") Losers,
    @SerialName("grand-final") GrandFinal,
    @SerialName("grand-final-reset") GrandFinalReset,
    @SerialName("third-place") ThirdPlace,
    @SerialName("swiss") Swiss,
}
