package de.mineking.hexo.hds.implementation.tournament

import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.TimeControl
import de.mineking.hexo.hds.model.game.FinishedGameRepository
import de.mineking.hexo.hds.model.game.GameReference
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.profile.ProfileRepository
import de.mineking.hexo.hds.model.session.SessionReference
import de.mineking.hexo.hds.model.session.SessionRepository
import de.mineking.hexo.hds.model.tournament.Tournament
import de.mineking.hexo.hds.model.tournament.TournamentFormat
import de.mineking.hexo.hds.model.tournament.TournamentId
import de.mineking.hexo.hds.model.tournament.TournamentMatch
import de.mineking.hexo.hds.model.tournament.TournamentMatchPlayer
import de.mineking.hexo.hds.model.tournament.TournamentParticipant
import de.mineking.hexo.hds.model.tournament.TournamentStanding
import de.mineking.hexo.hds.model.tournament.TournamentStatus

internal class TournamentImpl(
    override val id: TournamentId,
    override val url: String,
    override val name: String,
    override val description: String?,
    override val format: TournamentFormat,
    override val status: TournamentStatus,
    override val scheduledStartAt: Instant,
    override val checkInOpensAt: Instant,
    override val checkInClosesAt: Instant,
    override val maxPlayers: Int,
    override val registeredCount: Int,
    override val checkedInCount: Int,
    override val timeControl: TimeControl,
    override val participants: List<TournamentParticipant>,
    override val matches: List<TournamentMatch>,
) : Tournament {
    companion object {
        private fun TournamentDto.createParticipantList(repository: ProfileRepository): List<TournamentParticipant> {
            val participantsDtos = participants.associateBy { it.profileId }
            return standings.map {
                val participant = participantsDtos[it.profileId] ?: error("Couldn't find participant ${it.profileId}")
                TournamentParticipantImpl(
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

            return TournamentImpl(
                id = dto.id,
                url = "${client.host}/tournaments/${dto.id.value}",
                name = dto.name,
                description = dto.description,
                format = createTournamentFormat(dto, matches),
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

private class TournamentParticipantImpl(
    private val repository: ProfileRepository,
    override val profileId: ProfileId,
    override val displayName: String,
    override val image: String?,
    override val registeredAt: Instant,
    override val seed: Int?,
    override val standing: TournamentStanding,
) : TournamentParticipant {
    override suspend fun fetchProfile() = repository.getProfile(profileId)
}
