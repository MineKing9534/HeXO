package de.mineking.hexo.hds.implementation.session

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.GamePosition
import de.mineking.hexo.board.NextTurnMetaData
import de.mineking.hexo.board.findNextTurn
import de.mineking.hexo.board.toTurns
import de.mineking.hexo.game.model.LiveDuration
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.GameMove
import de.mineking.hexo.game.model.game.GameVisibility
import de.mineking.hexo.game.model.game.PlayerId
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.session.DetailedSession
import de.mineking.hexo.game.model.session.LiveSession
import de.mineking.hexo.game.model.session.LiveSessionPlayer
import de.mineking.hexo.game.model.session.LobbySession
import de.mineking.hexo.game.model.session.RatingAdjustment
import de.mineking.hexo.game.model.session.Session
import de.mineking.hexo.game.model.session.SessionGame
import de.mineking.hexo.game.model.session.SessionPlayer
import de.mineking.hexo.game.model.session.SessionPlayerConnectionStatus
import de.mineking.hexo.game.model.session.SessionState
import de.mineking.hexo.game.model.session.SessionTurn
import de.mineking.hexo.game.model.session.hasStarted
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.Instant
import de.mineking.hexo.hds.implementation.game.GameFinishReasonDto
import de.mineking.hexo.hds.implementation.game.GameOptionsDto
import de.mineking.hexo.hds.implementation.game.GameResultDto
import de.mineking.hexo.hds.implementation.game.PlayerImpl
import de.mineking.hexo.hds.implementation.game.TournamentMatchSnapshotDto
import de.mineking.hexo.hds.implementation.game.toModel
import de.mineking.hexo.utils.types.urlOf
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal abstract class BaseSessionImpl : Session {
    internal abstract val client: HdsApiClient

    override val url get() = client.sessionRepository.urlOf(id)

    override fun observe() = client.sessionRepository.observeSession(id)
}

internal class SessionImpl(
    override val client: HdsApiClient,
    private val dto: LobbyInfoDto,
) : BaseSessionImpl() {
    override val id = dto.id
    override val gameOptions = GameOptionsDto(
        rated = dto.rated,
        visibility = GameVisibility.Public,
        timeControl = dto.timeControl,
    )

    override val createdAt = dto.createdAt
    override val startedAt = dto.startedAt
    override val state = if (hasStarted()) SessionState.InGame else SessionState.Lobby

    override val tournament = null
    override val players = createPlayerList(client, null, dto.players)
}

internal class DetailedSessionImpl(
    override val client: HdsApiClient,
    private val dto: SessionDto,
) : BaseSessionImpl() {
    override val id = dto.id
    override val gameOptions = dto.gameOptions

    override val createdAt = Instant.DISTANT_PAST
    override val startedAt = if (dto.state is SessionStateDto.GameSessionState) Instant.DISTANT_PAST else null
    override val state = when (dto.state) {
        is SessionStateDto.Lobby -> SessionState.Lobby
        is SessionStateDto.InGame -> SessionState.InGame
        is SessionStateDto.Finished -> SessionState.Finished
    }

    override val tournament = dto.tournament?.toModel(client)
    override val players = createPlayerList(client, dto.tournament, dto.players)
}

internal abstract class ObservedSessionImpl : BaseSessionImpl(), DetailedSession {
    internal abstract val dto: SessionDto
    internal abstract val gameState: SessionGameStateDto

    companion object {
        internal fun of(
            client: HdsApiClient,
            dto: SessionDto,
            gameState: SessionGameStateDto,
        ) = when {
            dto.players.isEmpty() && dto.tournament == null -> ClosedSessionImpl(client, dto, dto.state, gameState)
            dto.state is SessionStateDto.Lobby -> LobbySessionImpl(client, dto, dto.state, gameState)
            dto.state is SessionStateDto.GameSessionState -> LiveSessionImpl(client, dto, dto.state, gameState)
            else -> error("impossible")
        }
    }
}

internal class ClosedSessionImpl(
    override val client: HdsApiClient,
    override val dto: SessionDto,
    stateDto: SessionStateDto,
    override val gameState: SessionGameStateDto,
) : ObservedSessionImpl() {
    override val id = dto.id
    override val createdAt = stateDto.createdAt
    override val startedAt = null
    override val gameOptions = dto.gameOptions
    override val tournament = dto.tournament?.toModel(client)

    override val state = SessionState.Closed
    override val players: List<SessionPlayer> = emptyList()
}

internal class LobbySessionImpl(
    override val client: HdsApiClient,
    override val dto: SessionDto,
    stateDto: SessionStateDto.Lobby,
    override val gameState: SessionGameStateDto,
) : ObservedSessionImpl(), LobbySession {
    override val id = dto.id
    override val createdAt = stateDto.createdAt
    override val gameOptions = dto.gameOptions
    override val tournament = dto.tournament?.toModel(client)

    override val players = createPlayerList(client, dto.tournament, dto.players)
}

private fun createPlayerList(
    client: HdsApiClient,
    tournament: TournamentMatchSnapshotDto?,
    players: List<AbstractSessionPlayerDto>,
) = if (tournament == null) {
    players.mapIndexed { index, data ->
        LobbySessionPlayerImpl(
            client = client,
            dto = data,
            color = CellOwner.entries[index],
            tournamentMatchWins = null,
        )
    }
} else {
    data class TournamentPlayer(
        override val profileId: ProfileId,
        override val displayName: String,
        val tournamentWins: Int,
        override val connectionStatus: SessionPlayerConnectionStatus,
    ) : AbstractSessionPlayerDto {
        override val playerId = PlayerId("")
        override val elo = players.find { it.profileId == profileId }?.elo ?: 0
    }

    listOf(
        TournamentPlayer(
            tournament.leftProfileId,
            tournament.leftDisplayName,
            tournament.leftWins,
            players.find { it.profileId == tournament.leftProfileId }?.connectionStatus
                ?: SessionPlayerConnectionStatus.Disconnected,
        ),
        TournamentPlayer(
            tournament.rightProfileId,
            tournament.rightDisplayName,
            tournament.rightWins,
            players.find { it.profileId == tournament.rightProfileId }?.connectionStatus
                ?: SessionPlayerConnectionStatus.Disconnected,
        ),
    ).mapIndexed { index, player ->
        LobbySessionPlayerImpl(
            client = client,
            dto = player,
            color = CellOwner.entries[(index + tournament.currentGameNumber + 1) % 2],
            tournamentMatchWins = player.tournamentWins,
        )
    }
}

internal class LobbySessionPlayerImpl(
    client: HdsApiClient,
    dto: AbstractSessionPlayerDto,
    override val color: CellOwner,
    override val tournamentMatchWins: Int?,
) : SessionPlayer, PlayerImpl(client.profileRepository, client.finishedGameRepository, dto) {
    override val id = PlayerId("")
    override val connectionStatus = dto.connectionStatus
}

private val graceTimePeriod = 10.seconds

internal class LiveSessionImpl(
    override val client: HdsApiClient,
    override val dto: SessionDto,
    private val stateDto: SessionStateDto.GameSessionState,
    override val gameState: SessionGameStateDto,
) : ObservedSessionImpl(), LiveSession {
    private val gracePeriodStartedAt = gameState.graceTimerStartedAt?.takeIf { gameState.currentTurnUsesGraceTime == true }
    private val activeGraceTimeRemaining = gracePeriodStartedAt?.let { startedAt ->
        val now = Clock.System.now()
        val remaining = (graceTimePeriod - (now - startedAt)).coerceIn(Duration.ZERO..graceTimePeriod)
        LiveDuration(remaining, now).takeIf { remaining > Duration.ZERO }
    }

    internal fun getPlayerById(id: PlayerId) = players.first { it.id == id }

    private fun extractTimeRemaining(player: SessionPlayerDto): LiveDuration? {
        val timeControl = dto.gameOptions.timeControl
        if (timeControl is TimeControl.Unlimited) return null

        val timeRemaining = baseTimeRemaining(player.id, timeControl)
            ?: return fallbackTimeRemaining(player.id, timeControl)

        val adjustedTime = adjustForGracePeriod(player.id, timeControl, timeRemaining)
        return freezeAtFinish(player.id, adjustedTime)
    }

    private fun baseTimeRemaining(playerId: PlayerId, timeControl: TimeControl): LiveDuration? {
        if (playerId == gameState.currentTurnPlayerId) {
            val timestamp = (stateDto as? SessionStateDto.Finished)?.finishedAt ?: Clock.System.now()
            return gameState.currentTurnExpiresAt?.durationSince(timestamp)
                ?: gameState.playerTimeRemaining[playerId]
        }

        return when (timeControl) {
            is TimeControl.Unlimited -> null
            is TimeControl.Turn -> LiveDuration(timeControl.turnTime, Clock.System.now())
            is TimeControl.Match -> gameState.playerTimeRemaining[playerId]
        }
    }

    private fun fallbackTimeRemaining(playerId: PlayerId, timeControl: TimeControl): LiveDuration? {
        val finishedState = stateDto as? SessionStateDto.Finished ?: return null

        return when {
            finishedState.finishReason == GameFinishReasonDto.Timeout && playerId != finishedState.winningPlayerId ->
                LiveDuration(Duration.ZERO, finishedState.finishedAt)

            timeControl is TimeControl.Turn -> LiveDuration(timeControl.turnTime, finishedState.finishedAt)
            else -> null
        }
    }

    private fun adjustForGracePeriod(
        playerId: PlayerId,
        timeControl: TimeControl,
        timeRemaining: LiveDuration,
    ): LiveDuration {
        if (playerId != gameState.currentTurnPlayerId || gracePeriodStartedAt == null) return timeRemaining

        val duration = when (timeControl) {
            is TimeControl.Unlimited -> return timeRemaining
            is TimeControl.Turn -> timeControl.turnTime
            is TimeControl.Match -> {
                val playerTime = gameState.playerTimeRemaining[playerId]?.duration ?: timeRemaining.duration
                (playerTime - graceTimePeriod).coerceAtLeast(Duration.ZERO)
            }
        }

        return LiveDuration(duration, gracePeriodStartedAt + graceTimePeriod)
    }

    private fun freezeAtFinish(playerId: PlayerId, timeRemaining: LiveDuration): LiveDuration {
        val finishedState = stateDto as? SessionStateDto.Finished ?: return timeRemaining
        if (playerId != gameState.currentTurnPlayerId) return timeRemaining

        val elapsed = (finishedState.finishedAt - timeRemaining.timestamp).coerceAtLeast(Duration.ZERO)
        return LiveDuration(
            duration = (timeRemaining.duration - elapsed).coerceAtLeast(Duration.ZERO),
            timestamp = finishedState.finishedAt,
        )
    }

    override val id = dto.id
    override val gameOptions = dto.gameOptions
    override val tournament = dto.tournament?.toModel(client)

    override val createdAt = stateDto.createdAt
    override val startedAt = stateDto.startedAt

    override val players = dto.players.mapIndexed { index, data ->
        val color = gameState.playerTiles?.get(data.id)?.color ?: CellOwner.entries[index]

        LiveSessionPlayerImpl(
            client = client,
            dto = data,
            color = color,
            tournamentMatchWins = dto.tournament?.let {
                when (data.profileId) {
                    it.leftProfileId -> it.leftWins
                    it.rightProfileId -> it.rightWins
                    else -> error("Inconsistent tournament snapshot")
                }
            },
            timeRemaining = extractTimeRemaining(data),
            graceTimeRemaining = when {
                stateDto !is SessionStateDto.InGame -> null
                dto.gameOptions.timeControl is TimeControl.Unlimited -> null
                data.id == gameState.currentTurnPlayerId -> activeGraceTimeRemaining
                gameState.hasUsedGracePeriod[data.id] != true -> LiveDuration(graceTimePeriod, Clock.System.now())
                else -> null
            },
        )
    }

    override val state = when (stateDto) {
        is SessionStateDto.InGame -> SessionState.Detailed.InGame(
            currentTurn = SessionTurn(
                player = getPlayerById(gameState.currentTurnPlayerId!!),
                placementsRemaining = gameState.placementsRemaining,
                expiresIn = gameState.currentTurnExpiresAt?.durationSince(Clock.System.now()),
            ),
        )

        is SessionStateDto.Finished -> SessionState.Detailed.Finished(
            result = GameResultDto(
                winningPlayerId = stateDto.winningPlayerId,
                abortedByPlayerId = stateDto.abortedByPlayerId,
                duration = stateDto.finishedAt - stateDto.startedAt,
                reason = stateDto.finishReason,
            ).toModel(players),
            rematchAcceptedPlayers = stateDto.rematchAcceptedPlayerIds.map { getPlayerById(it) },
        )
    }

    override val game = SessionGameImpl(this)
}

internal class LiveSessionPlayerImpl(
    client: HdsApiClient,
    dto: SessionPlayerDto,
    override val color: CellOwner,
    override val tournamentMatchWins: Int?,
    override val timeRemaining: LiveDuration?,
    override val graceTimeRemaining: LiveDuration?,
) : LiveSessionPlayer, PlayerImpl(client.profileRepository, client.finishedGameRepository, dto) {
    override val elo = dto.elo.takeIf { it != 0 }
    override val ratingAdjustment = dto.ratingAdjustment?.let { RatingAdjustment(eloGain = it.eloGain, eloLoss = it.eloLoss) }
    override val connectionStatus = dto.connectionStatus
}

internal class SessionGameImpl(
    session: LiveSessionImpl,
) : SessionGame {
    override val id = (session.dto.state as SessionStateDto.GameSessionState).gameId
    override val startedAt = session.startedAt
    override val result = (session.state as? SessionState.Detailed.Finished)?.result
    override val options = session.dto.gameOptions
    override val players = session.players.sortedBy { player ->
        session.gameState.cells
            ?.indexOfFirst { it.occupiedBy == player.id }
            ?.takeIf { it >= 0 }
            ?: Int.MAX_VALUE
    }
    override val tournament = session.tournament
    override val position = session.gameState.cells!!.map { move ->
        GameMove(
            coordinate = CellCoordinate(move.q, move.r),
            player = session.getPlayerById(move.occupiedBy),
        )
    }.toTurns().let { turns ->
        GamePosition(
            turns = turns,
            nextTurn = session.gameState.currentTurnPlayerId?.let { id ->
                NextTurnMetaData(
                    player = session.getPlayerById(id).color,
                    placementsRemaining = session.gameState.placementsRemaining,
                    turn = session.gameState.turnCount,
                )
            } ?: turns.findNextTurn(hasState = false),
        )
    }
}

private fun Instant.durationSince(timestamp: Instant): LiveDuration {
    return LiveDuration(this - timestamp, timestamp)
}
