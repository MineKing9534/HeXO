package de.mineking.hexo.hds.model.tournament

import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.TimeControl
import de.mineking.hexo.hds.model.game.GameReference
import de.mineking.hexo.hds.model.profile.Profile
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.session.SessionReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline
@Serializable
value class TournamentId(val value: Uuid)

@JvmInline
@Serializable
value class TournamentMatchId(val value: String)

interface Tournament {
    val id: TournamentId
    val url: String
    val name: String
    val description: String?
    val format: TournamentFormat
    val status: TournamentStatus
    val scheduledStartAt: Instant
    val checkInOpensAt: Instant
    val checkInClosesAt: Instant
    val maxPlayers: Int
    val registeredCount: Int
    val checkedInCount: Int
    val timeControl: TimeControl
    val participants: List<TournamentParticipant>
    val matches: List<TournamentMatch>
}

interface TournamentParticipant {
    val profileId: ProfileId
    val displayName: String
    val image: String?
    val registeredAt: Instant
    val seed: Int?
    val standing: TournamentStanding

    suspend fun fetchProfile(): Profile?
}

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
enum class TournamentBracket {
    @SerialName("winners") Winners,
    @SerialName("losers") Losers,
    @SerialName("grand-final") GrandFinal,
    @SerialName("grand-final-reset") GrandFinalReset,
    @SerialName("third-place") ThirdPlace,
    @SerialName("swiss") Swiss,
}

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

data class TournamentMatchPlayer(
    val participant: TournamentParticipant?,
    val seed: Int?,
    val isByte: Boolean,
    val wins: Int,
    val isWinner: Boolean?,
    val currentColor: CellOwner,
)

data class TournamentMatch(
    val id: TournamentMatchId,
    val bracket: TournamentBracket,
    val round: Int,
    val order: Int,
    val state: TournamentMatchState,
    val bestOf: Int,
    val currentGameNumber: Int,
    val winner: TournamentParticipant?,
    val resultType: TournamentMatchResultType?,
    val waitingForPlayers: Boolean,
    val startedAt: Instant?,
    val resolvedAt: Instant?,
    val players: List<TournamentMatchPlayer>,
    val pastGames: List<GameReference>,
    val session: SessionReference?,
)
