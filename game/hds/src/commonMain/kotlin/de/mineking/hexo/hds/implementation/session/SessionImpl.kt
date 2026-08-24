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
import de.mineking.hexo.game.model.session.LiveSession
import de.mineking.hexo.game.model.session.LiveSessionPlayer
import de.mineking.hexo.game.model.session.LobbySession
import de.mineking.hexo.game.model.session.RatingAdjustment
import de.mineking.hexo.game.model.session.Session
import de.mineking.hexo.game.model.session.SessionGame
import de.mineking.hexo.game.model.session.SessionState
import de.mineking.hexo.game.model.session.SessionTurn
import de.mineking.hexo.hds.implementation.Duration
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.Instant
import de.mineking.hexo.hds.implementation.game.GameOptionsDto
import de.mineking.hexo.hds.implementation.game.PlayerImpl
import de.mineking.hexo.hds.implementation.game.TournamentMatchSnapshotDto
import de.mineking.hexo.hds.implementation.game.toModel
import kotlin.time.Clock

internal abstract class SessionImpl : Session {
    internal abstract val client: HdsApiClient
    internal abstract val dto: SessionDto?
    internal abstract val gameState: SessionGameStateDto?

    override fun observe() = client.sessionRepository.observeSession(id)

    companion object {
        internal fun of(
            client: HdsApiClient,
            dto: SessionDto,
            lastState: Pair<Instant, SessionStateDto.InGame>?,
            gameState: SessionGameStateDto,
        ) = when (dto.state) {
            is SessionStateDto.Lobby -> LobbySessionImpl(client, dto, gameState)
            is SessionStateDto.GameSessionState -> LiveSessionImpl(client, dto, lastState, gameState)
        }
    }
}

internal abstract class AbstractLobbySessionImpl(
    tournament: TournamentMatchSnapshotDto?,
    players: List<AbstractSessionPlayer>,
) : SessionImpl(), LobbySession {
    override val state = SessionState.Lobby
    override val startedAt = null

    override val players = if (tournament == null) {
        players.mapIndexed { index, data ->
            LobbySessionPlayerImpl(
                client = client,
                profileId = data.profileId,
                displayName = data.displayName,
                elo = data.elo,
                color = CellOwner.entries[index],
                tournamentMatchWins = null,
            )
        }
    } else {
        data class TournamentPlayer(
            val profileId: ProfileId,
            val displayName: String,
            val tournamentWins: Int,
        )

        listOf(
            TournamentPlayer(tournament.leftProfileId, tournament.leftDisplayName, tournament.leftWins),
            TournamentPlayer(tournament.rightProfileId, tournament.rightDisplayName, tournament.rightWins),
        ).mapIndexed { index, player ->
            LobbySessionPlayerImpl(
                client = client,
                profileId = player.profileId,
                displayName = player.displayName,
                elo = players.find { it.profileId == player.profileId }?.elo ?: -1,
                color = CellOwner.entries[(index + tournament.currentGameNumber + 1) % 2],
                tournamentMatchWins = player.tournamentWins,
            )
        }
    }
}

internal class LobbySessionImpl(
    override val client: HdsApiClient,
    override val dto: SessionDto,
    override val gameState: SessionGameStateDto,
) : AbstractLobbySessionImpl(dto.tournament, dto.players) {
    override val id = dto.id
    override val createdAt = Instant.DISTANT_PAST
    override val gameOptions = dto.gameOptions
    override val tournament = dto.tournament?.toModel(client)
}

internal class LobbyListSessionImpl(
    override val client: HdsApiClient,
    dto: LobbyInfoDto,
) : AbstractLobbySessionImpl(null, dto.players) {
    override val id = dto.id
    override val gameOptions = GameOptionsDto(
        rated = dto.rated,
        visibility = GameVisibility.Public,
        timeControl = dto.timeControl,
    )

    override val tournament = null

    override val createdAt = dto.createdAt

    override val dto = null
    override val gameState = null
}

internal class LobbySessionPlayerImpl(
    client: HdsApiClient,
    override val profileId: ProfileId?,
    override val displayName: String,
    override val color: CellOwner,
    override val elo: Int,
    override val tournamentMatchWins: Int?,
) : PlayerImpl(client.profileRepository) {
    override val playerId = PlayerId("")
}

internal class LiveSessionImpl(
    override val client: HdsApiClient,
    override val dto: SessionDto,
    internal val lastState: Pair<Instant, SessionStateDto.InGame>?,
    override val gameState: SessionGameStateDto,
) : SessionImpl(), LiveSession {
    internal fun getPlayerById(id: PlayerId) = players.first { it.playerId == id }

    override val id = dto.id
    override val gameOptions = dto.gameOptions
    override val tournament = dto.tournament?.toModel(client)

    override val createdAt = Instant.DISTANT_PAST
    override val startedAt = when (dto.state) {
        is SessionStateDto.Lobby -> error("")
        is SessionStateDto.InGame -> dto.state.startedAt
        is SessionStateDto.Finished -> lastState?.second?.startedAt ?: Instant.DISTANT_PAST
    }

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

    override val state = when (dto.state) {
        is SessionStateDto.Lobby -> error("")

        is SessionStateDto.InGame -> SessionState.InGame(
            currentTurn = SessionTurn(
                player = getPlayerById(gameState.currentTurnPlayerId!!),
                placementsRemaining = gameState.placementsRemaining,
                expiresIn = gameState.currentTurnExpiresIn,
            ),
        )

        is SessionStateDto.Finished -> SessionState.Finished(
            result = GameResult(
                winner = dto.state.winningPlayerId?.let { getPlayerById(it) },
                duration = lastState?.let { it.first - it.second.startedAt } ?: Duration.ZERO,
                reason = dto.state.finishReason,
            ),
            rematchAcceptedPlayers = dto.state.rematchAcceptedPlayerIds.map { getPlayerById(it) },
        )
    }

    override val game = SessionGameImpl(this)
}

internal class LiveSessionPlayerImpl(
    private val client: HdsApiClient,
    private val dto: SessionPlayerDto,
    override val color: CellOwner,
    override val tournamentMatchWins: Int?,
    override val timeRemaining: LiveDuration?,
) : LiveSessionPlayer, PlayerImpl(client.profileRepository) {
    override val playerId = dto.id
    override val profileId = dto.profileId
    override val displayName = dto.displayName
    override val elo = dto.rating.eloScore
    override val ratingAdjustment = dto.ratingAdjustment?.let { RatingAdjustment(eloGain = it.eloGain, eloLoss = it.eloLoss) }
    override val connectionStatus = dto.connection.status
}

internal class SessionGameImpl(
    session: LiveSessionImpl,
) : SessionGame {
    override val id = (session.dto.state as SessionStateDto.GameSessionState).gameId
    override val startedAt = session.startedAt
    override val result = (session.state as? SessionState.Finished)?.result
    override val options = session.dto.gameOptions
    override val players = session.players.sortedBy { player -> session.gameState.cells?.indexOfFirst { it.occupiedBy == player.playerId } }
    override val tournament = session.tournament
    override val position = session.gameState.cells!!.map { move ->
        GameMove(
            coordinate = CellCoordinate(move.q, move.r),
            player = session.getPlayerById(move.occupiedBy),
        )
    }.toGamePosition()
}
