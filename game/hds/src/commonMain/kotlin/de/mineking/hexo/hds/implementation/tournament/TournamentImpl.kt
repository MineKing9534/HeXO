package de.mineking.hexo.hds.implementation.tournament

import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.game.model.game.GameReference
import de.mineking.hexo.game.model.session.SessionReference
import de.mineking.hexo.game.model.tournament.Tournament
import de.mineking.hexo.game.model.tournament.TournamentInfo
import de.mineking.hexo.game.model.tournament.TournamentMatch
import de.mineking.hexo.game.model.tournament.TournamentMatchInfo
import de.mineking.hexo.game.model.tournament.TournamentMatchPlayer
import de.mineking.hexo.game.model.tournament.TournamentMatchResult
import de.mineking.hexo.game.model.tournament.TournamentMatchState
import de.mineking.hexo.game.model.tournament.TournamentParticipant
import de.mineking.hexo.game.model.tournament.TournamentStanding
import de.mineking.hexo.hds.implementation.HdsApiClient

internal class TournamentImpl(
    private val client: HdsApiClient,
    private val dto: TournamentDto,
) : Tournament {
    override val info = TournamentInfo(
        id = dto.id,
        url = "${client.host}/tournaments/${dto.id.value}",
        name = dto.name,
    )
    override val description = dto.description
    override val status = dto.status.model
    override val scheduledStartAt = dto.scheduledStartAt
    override val checkInOpensAt = dto.checkInOpensAt
    override val checkInClosesAt = dto.checkInClosesAt
    override val maxPlayers = dto.maxPlayers
    override val registeredCount = dto.registeredCount
    override val checkedInCount = dto.checkedInCount
    override val timeControl = dto.timeControl

    override val participants = dto.standings.map { standing ->
        val participant = dto.participants.first { it.profileId == standing.profileId }
        TournamentParticipantImpl(
            client = client,
            dto = participant,
            standing = TournamentStanding(
                rank = standing.rank,
                wins = standing.wins,
                losses = standing.losses,
            ),
        )
    }.sortedBy { it.standing.rank }

    override val matches = dto.matches.map { TournamentMatchImpl(client, it, participants) }
    override val format = createTournamentFormat(dto, matches)
}

internal class TournamentParticipantImpl(
    private val client: HdsApiClient,
    private val dto: TournamentParticipantDto,
    override val standing: TournamentStanding,
) : TournamentParticipant {
    override val profileId = dto.profileId
    override val displayName = dto.displayName
    override val image = dto.image
    override val registeredAt = dto.registeredAt
    override val seed = dto.seed

    override suspend fun fetchProfile() = client.profileRepository.getProfile(profileId)
}

internal class TournamentMatchPlayerImpl(
    private val dto: TournamentMatchSlotDto,
    override val participant: TournamentParticipant,
    override val wins: Int,
    override val currentColor: CellOwner,
) : TournamentMatchPlayer.Participant {
    override val seed = dto.seed!!
}

internal class TournamentMatchImpl(
    private val client: HdsApiClient,
    private val dto: TournamentMatchDto,
    private val participants: List<TournamentParticipant>,
) : TournamentMatch {
    override val info = TournamentMatchInfo(
        id = dto.id,
        bracket = dto.bracket.model,
        round = dto.round,
        order = dto.order,
        bestOf = dto.bestOf,
        currentGameNumber = dto.currentGameNumber,
    )

    override val startedAt = dto.startedAt
    override val resolvedAt = dto.resolvedAt

    override val pastGames = dto.gameIds.map { GameReference(client.finishedGameRepository, it) }

    override val players = dto.slots.mapIndexed { index, slot ->
        val wins = when (index) {
            0 -> dto.leftWins
            1 -> dto.rightWins
            else -> error("Unexpected slot index $index")
        }
        val currentColor = CellOwner.entries[(index + dto.currentGameNumber + 1) % 2]

        if (slot.isBye) {
            return@mapIndexed TournamentMatchPlayer.Bye(wins, currentColor)
        }

        TournamentMatchPlayerImpl(
            dto = slot,
            participant = participants.first { it.profileId == slot.profileId },
            wins = wins,
            currentColor = currentColor,
        )
    }

    override val state = when (dto.state) {
        TournamentMatchStateDto.Pending -> TournamentMatchState.Pending
        TournamentMatchStateDto.Ready -> TournamentMatchState.Ready
        TournamentMatchStateDto.InProgress -> TournamentMatchState.InProgress(SessionReference(client.sessionRepository, dto.sessionId!!))
        TournamentMatchStateDto.Completed -> TournamentMatchState.Completed(
            TournamentMatchResult(
                type = dto.resultType!!.model,
                winner = players
                    .filterIsInstance<TournamentMatchPlayer.Participant>()
                    .first { it.profileId == dto.winnerProfileId }.participant,
            ),
        )
    }
}
