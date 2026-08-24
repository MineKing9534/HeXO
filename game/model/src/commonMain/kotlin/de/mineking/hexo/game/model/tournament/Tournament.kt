package de.mineking.hexo.game.model.tournament

import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.GameReference
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.session.SessionReference
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlin.uuid.Uuid

@JvmInline
@Serializable
value class TournamentId(val value: Uuid)

@JvmInline
@Serializable
value class TournamentMatchId(val value: String)

data class TournamentInfo(
    val id: TournamentId,
    val url: String,
    val name: String,
)

interface Tournament {
    val id get() = info.id
    val info: TournamentInfo
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

enum class TournamentStatus {
    Draft,
    RegistrationOpen,
    CheckInOpen,
    Live,
    Completed,
    Cancelled,
}

fun TournamentStatus.isTerminal() = this >= TournamentStatus.Completed

enum class TournamentBracket {
    Default,
    Losers,
    GrandFinal,
    ThirdPlace,
}

data class TournamentStanding(
    val rank: Int,
    val wins: Int,
    val losses: Int,
)

data class TournamentMatchInfo(
    val id: TournamentMatchId,
    val bracket: TournamentBracket,
    val round: Int,
    val order: Int,
    val bestOf: Int,
    val currentGameNumber: Int,
)

val TournamentMatchInfo.requiredWins get() = bestOf / 2 + 1

sealed interface TournamentMatchPlayer {
    val wins: Int
    val currentColor: CellOwner

    val profileId: ProfileId?

    class Bye(
        override val wins: Int,
        override val currentColor: CellOwner,
    ) : TournamentMatchPlayer {
        override val profileId = null
    }

    interface Participant : TournamentMatchPlayer {
        val participant: TournamentParticipant
        val seed: Int

        override val profileId get() = participant.profileId
    }
}

enum class TournamentMatchResultType {
    Played,
    Bye,
    Walkover,
}

data class TournamentMatchResult(
    val winner: TournamentParticipant,
    val type: TournamentMatchResultType,
)

sealed interface TournamentMatchState {
    object Pending : TournamentMatchState
    object Ready : TournamentMatchState
    class InProgress(val session: SessionReference) : TournamentMatchState
    class Completed(val result: TournamentMatchResult) : TournamentMatchState
}

interface TournamentMatch {
    val info: TournamentMatchInfo
    val state: TournamentMatchState
    val startedAt: Instant?
    val resolvedAt: Instant?
    val players: List<TournamentMatchPlayer>
    val pastGames: List<GameReference>
}

val TournamentMatch.result get() = (state as? TournamentMatchState.Completed)?.result
val TournamentMatch.session get() = (state as? TournamentMatchState.InProgress)?.session
