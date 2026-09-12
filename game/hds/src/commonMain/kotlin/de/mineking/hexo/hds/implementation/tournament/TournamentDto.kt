package de.mineking.hexo.hds.implementation.tournament

import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.game.model.tournament.TournamentBracket
import de.mineking.hexo.game.model.tournament.TournamentId
import de.mineking.hexo.game.model.tournament.TournamentMatchId
import de.mineking.hexo.game.model.tournament.TournamentMatchResultType
import de.mineking.hexo.game.model.tournament.TournamentStatus
import de.mineking.hexo.hds.implementation.Instant
import de.mineking.hexo.hds.implementation.TimeControl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class TournamentFormatType {
    @SerialName("single-elimination") SingleElimination,
    @SerialName("double-elimination") DoubleElimination,
    @SerialName("swiss") Swiss,
}

@Serializable
internal enum class TournamentStatusDto(val model: TournamentStatus) {
    @SerialName("draft") Draft(TournamentStatus.Draft),
    @SerialName("registration-open") RegistrationOpen(TournamentStatus.RegistrationOpen),
    @SerialName("check-in-open") CheckInOpen(TournamentStatus.CheckInOpen),
    @SerialName("waitlist-open") WaitlistOpen(TournamentStatus.CheckInOpen),
    @SerialName("live") Live(TournamentStatus.Live),
    @SerialName("completed") Completed(TournamentStatus.Completed),
    @SerialName("cancelled") Cancelled(TournamentStatus.Cancelled),
}

@Serializable
enum class TournamentBracketDto(val model: TournamentBracket) {
    @SerialName("winners") Winners(TournamentBracket.Default),
    @SerialName("losers") Losers(TournamentBracket.Losers),
    @SerialName("grand-final") GrandFinal(TournamentBracket.GrandFinal),
    @SerialName("grand-final-reset") GrandFinalReset(TournamentBracket.GrandFinal),
    @SerialName("third-place") ThirdPlace(TournamentBracket.ThirdPlace),
    @SerialName("swiss") Swiss(TournamentBracket.Default),
}

@Serializable
enum class TournamentMatchStateDto {
    @SerialName("pending") Pending,
    @SerialName("ready") Ready,
    @SerialName("in-progress") InProgress,
    @SerialName("completed") Completed,
}

@Serializable
enum class TournamentMatchResultTypeDto(val model: TournamentMatchResultType) {
    @SerialName("played") Played(TournamentMatchResultType.Played),
    @SerialName("bye") Bye(TournamentMatchResultType.Bye),
    @SerialName("walkover") Walkover(TournamentMatchResultType.Walkover),
}

@Serializable
data class TournamentStandingDto(
    val rank: Int,
    val profileId: ProfileId,
    val wins: Int,
    val losses: Int,
    val buchholz: Int,
    val sonnebornBerger: Int,
)

@Serializable
internal data class TournamentDto(
    val id: TournamentId,
    val name: String,
    val description: String?,
    val format: TournamentFormatType,
    val status: TournamentStatusDto,
    val scheduledStartAt: Instant,
    val checkInOpensAt: Instant,
    val checkInClosesAt: Instant,
    val maxPlayers: Int,
    val registeredCount: Int,
    val checkedInCount: Int,
    val timeControl: TimeControl,
    val participants: List<TournamentParticipantDto>,
    val standings: List<TournamentStandingDto>,
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
    val bracket: TournamentBracketDto,
    val round: Int,
    val order: Int,
    val state: TournamentMatchStateDto,
    val bestOf: Int,
    val currentGameNumber: Int,
    val leftWins: Int,
    val rightWins: Int,
    val winnerProfileId: ProfileId?,
    val loserProfileId: ProfileId?,
    val resultType: TournamentMatchResultTypeDto?,
    val startedAt: Instant?,
    val resolvedAt: Instant?,
    val slots: List<TournamentMatchSlotDto>,
    val gameIds: List<GameId>,
    val sessionId: SessionId?,
)
