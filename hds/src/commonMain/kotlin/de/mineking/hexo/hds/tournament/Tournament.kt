package de.mineking.hexo.hds.tournament

import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.game.FinishedGameRepository
import de.mineking.hexo.hds.game.GameReference
import de.mineking.hexo.hds.profile.ProfileId
import de.mineking.hexo.hds.profile.ProfileRepository
import de.mineking.hexo.hds.session.SessionReference
import de.mineking.hexo.hds.session.SessionRepository
import de.mineking.hexo.hds.utils.Instant
import de.mineking.hexo.hds.utils.TimeControl
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@JvmInline
@Serializable
value class TournamentId(val value: Uuid)

@JvmInline
@Serializable
value class TournamentMatchId(val value: String)

class Tournament private constructor(
    val id: TournamentId,
    val url: String,
    val name: String,
    val description: String?,
    val format: TournamentFormat,
    val status: TournamentStatus,
    val scheduledStartAt: Instant,
    val checkInOpensAt: Instant,
    val checkInClosesAt: Instant,
    val maxPlayers: Int,
    val registeredCount: Int,
    val checkedInCount: Int,
    val timeControl: TimeControl,
    val participants: List<TournamentParticipant>,
    val matches: List<TournamentMatch>,
) {
    companion object {
        private fun TournamentDto.createParticipantList(repository: ProfileRepository): List<TournamentParticipant> {
            val participantsDtos = participants.associateBy { it.profileId }
            return standings.map {
                val participant = participantsDtos[it.profileId] ?: error("Couldn't find participant ${it.profileId}")
                TournamentParticipant(
                    repository = repository,
                    profileId = participant.profileId,
                    displayName = participant.displayName,
                    image = participant.image,
                    registeredAt = participant.registeredAt,
                    seed = participant.seed,
                    standing = it,
                )
            }.sortedBy { it.standing.rank }
        }

        private fun TournamentDto.createMatchList(
            finishedGameRepository: FinishedGameRepository,
            sessionRepository: SessionRepository,
            participants: List<TournamentParticipant>,
        ): List<TournamentMatch> {
            val participantsById = participants.associateBy { it.profileId }

            return matches.map { match ->
                val players = match.slots.mapIndexed { index, slot ->
                    TournamentMatchPlayer(
                        participant = participantsById[slot.profileId],
                        isByte = slot.isBye,
                        wins = when (index) {
                            0 -> match.leftWins
                            1 -> match.rightWins
                            else -> error("Unexpected slot index $index")
                        },
                        isWinner = when (match.winnerProfileId) {
                            null -> null
                            slot.profileId -> true
                            else -> false
                        },
                        seed = slot.seed,
                        currentColor = CellOwner.entries[(index + match.currentGameNumber + 1) % 2],
                    )
                }

                TournamentMatch(
                    id = match.id,
                    bracket = match.bracket,
                    round = match.round,
                    order = match.order,
                    state = match.state,
                    bestOf = match.bestOf,
                    currentGameNumber = match.currentGameNumber,
                    resultType = match.resultType,
                    waitingForPlayers = match.waitingForPlayers,
                    startedAt = match.startedAt,
                    resolvedAt = match.resolvedAt,
                    pastGames = match.gameIds.map { GameReference(finishedGameRepository, it) },
                    session = match.sessionId?.let { SessionReference(sessionRepository, it) },
                    players = players,
                    winner = participantsById[match.winnerProfileId],
                )
            }
        }

        internal fun of(
            client: HdsApiClient,
            dto: TournamentDto,
        ): Tournament {
            val participants = dto.createParticipantList(client.profileRepository)
            val matches = dto.createMatchList(client.finishedGameRepository, client.sessionRepository, participants)

            return Tournament(
                id = dto.id,
                url = "${client.host}/tournaments/${dto.id.value}",
                name = dto.name,
                description = dto.description,
                format = TournamentFormat.of(dto, matches),
                status = dto.status,
                scheduledStartAt = dto.scheduledStartAt,
                checkInOpensAt = dto.checkInOpensAt,
                checkInClosesAt = dto.checkInClosesAt,
                maxPlayers = dto.maxPlayers,
                registeredCount = dto.registeredCount,
                checkedInCount = dto.checkedInCount,
                timeControl = dto.timeControl,
                participants = participants,
                matches = matches,
            )
        }
    }
}

class TournamentParticipant(
    private val repository: ProfileRepository,
    val profileId: ProfileId,
    val displayName: String,
    val image: String?,
    val registeredAt: Instant,
    val seed: Int?,
    val standing: TournamentStanding,
) {
    suspend fun fetchProfile() = repository.getProfile(profileId)
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
