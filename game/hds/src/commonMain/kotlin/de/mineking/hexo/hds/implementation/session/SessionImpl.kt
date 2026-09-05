package de.mineking.hexo.hds.implementation.session

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.toGamePosition
import de.mineking.hexo.game.model.LiveDuration
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.GameMove
import de.mineking.hexo.game.model.game.GameResult
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
import de.mineking.hexo.game.model.urlOf
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.Instant
import de.mineking.hexo.hds.implementation.game.GameOptionsDto
import de.mineking.hexo.hds.implementation.game.PlayerImpl
import de.mineking.hexo.hds.implementation.game.TournamentMatchSnapshotDto
import de.mineking.hexo.hds.implementation.game.toModel
import kotlin.time.Clock

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

internal class LiveSessionImpl(
    override val client: HdsApiClient,
    override val dto: SessionDto,
    stateDto: SessionStateDto.GameSessionState,
    override val gameState: SessionGameStateDto,
) : ObservedSessionImpl(), LiveSession {
    internal fun getPlayerById(id: PlayerId) = players.first { it.id == id }

    override val id = dto.id
    override val gameOptions = dto.gameOptions
    override val tournament = dto.tournament?.toModel(client)

    override val createdAt = stateDto.createdAt
    override val startedAt = stateDto.startedAt

    override val players = dto.players.mapIndexed { index, data ->
        val owner = gameState.playerTiles?.get(data.id)?.color?.let {
            when {
                it.red > 200 -> CellOwner.X
                it.blue > 200 -> CellOwner.O
                else -> error("Unrecognized color '${it.format()}'")
            }
        } ?: CellOwner.entries[index]

        LiveSessionPlayerImpl(
            client = client,
            dto = data,
            color = owner,
            tournamentMatchWins = dto.tournament?.let {
                when (data.profileId) {
                    it.leftProfileId -> it.leftWins
                    it.rightProfileId -> it.rightWins
                    else -> error("Inconsistent tournament snapshot")
                }
            },
            timeRemaining = when (dto.gameOptions.timeControl) {
                is TimeControl.Unlimited -> null
                is TimeControl.Turn ->
                    if (data.id == gameState.currentTurnPlayerId) {
                        gameState.currentTurnExpiresIn
                    } else {
                        LiveDuration(dto.gameOptions.timeControl.turnTime, Clock.System.now())
                    }
                is TimeControl.Match ->
                    if (data.id == gameState.currentTurnPlayerId) {
                        gameState.currentTurnExpiresIn
                    } else {
                        gameState.playerTimeRemaining[data.id]
                    }
            },
        )
    }

    override val state = when (stateDto) {
        is SessionStateDto.InGame -> SessionState.Detailed.InGame(
            currentTurn = SessionTurn(
                player = getPlayerById(gameState.currentTurnPlayerId!!),
                placementsRemaining = gameState.placementsRemaining,
                expiresIn = gameState.currentTurnExpiresIn,
            ),
        )

        is SessionStateDto.Finished -> SessionState.Detailed.Finished(
            result = GameResult(
                winner = stateDto.winningPlayerId?.let { getPlayerById(it) },
                duration = stateDto.finishedAt - stateDto.startedAt,
                reason = stateDto.finishReason.model,
            ),
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
) : LiveSessionPlayer, PlayerImpl(client.profileRepository, client.finishedGameRepository, dto) {
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
    override val players = session.players.sortedBy { player -> session.gameState.cells?.indexOfFirst { it.occupiedBy == player.id } }
    override val tournament = session.tournament
    override val position = session.gameState.cells!!.map { move ->
        GameMove(
            coordinate = CellCoordinate(move.q, move.r),
            player = session.getPlayerById(move.occupiedBy),
        )
    }.toGamePosition()
}
