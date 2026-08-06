package de.mineking.hexo.hds.implementation.session

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.hds.implementation.game.PlayerImpl
import de.mineking.hexo.hds.implementation.game.TournamentMatchSnapshotDto
import de.mineking.hexo.hds.implementation.game.TournamentMatchSnapshotImpl
import de.mineking.hexo.hds.model.Duration
import de.mineking.hexo.hds.model.LiveDuration
import de.mineking.hexo.hds.model.TimeControl
import de.mineking.hexo.hds.model.game.GameId
import de.mineking.hexo.hds.model.game.GameMove
import de.mineking.hexo.hds.model.game.GameOptions
import de.mineking.hexo.hds.model.game.GameResult
import de.mineking.hexo.hds.model.game.GameVisibility
import de.mineking.hexo.hds.model.game.Player
import de.mineking.hexo.hds.model.game.PlayerId
import de.mineking.hexo.hds.model.game.TournamentMatchSnapshot
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.profile.ProfileRepository
import de.mineking.hexo.hds.model.session.LiveSession
import de.mineking.hexo.hds.model.session.LiveSessionPlayer
import de.mineking.hexo.hds.model.session.LobbySession
import de.mineking.hexo.hds.model.session.Session
import de.mineking.hexo.hds.model.session.SessionGame
import de.mineking.hexo.hds.model.session.SessionId
import de.mineking.hexo.hds.model.session.SessionPlayerConnectionStatus
import de.mineking.hexo.hds.model.session.SessionRepository
import de.mineking.hexo.hds.model.session.SessionState
import de.mineking.hexo.hds.model.session.SessionTurn
import kotlin.time.Clock
import kotlin.time.Instant

internal abstract class SessionImpl : Session {
    internal abstract val dto: SessionDto?
    internal abstract val gameState: SessionGameStateDto?

    companion object {
        internal fun of(
            client: HdsApiClient,
            dto: SessionDto,
            lastState: Pair<Instant, SessionStateDto.InGame>?,
            gameState: SessionGameStateDto,
        ) = when (dto.state) {
            is SessionStateDto.Lobby -> LobbySessionImpl.of(client, dto, gameState)
            is SessionStateDto.GameSessionState -> LiveSessionImpl.of(client, dto, lastState, dto.state, gameState)
        }
    }
}

internal class LobbySessionImpl private constructor(
    private val repository: SessionRepository,
    override val id: SessionId,
    override val gameOptions: GameOptions,
    override val tournamentInfo: TournamentMatchSnapshot?,
    override val players: List<Player>,
    override val createdAt: Instant,
    override val startedAt: Instant?,
    override val dto: SessionDto?,
    override val gameState: SessionGameStateDto?,
) : SessionImpl(), LobbySession {
    override val state = SessionState.Lobby

    override fun observe() = repository.observeSession(id)

    companion object {
        private fun createPlayerList(
            repository: ProfileRepository,
            players: List<ISessionPlayerDto>,
            tournament: TournamentMatchSnapshotDto?,
        ): List<Player> {
            if (tournament == null) {
                return players.mapIndexed { index, data ->
                    PlayerImpl(
                        repository = repository,
                        playerId = PlayerId(""),
                        profileId = data.profileId,
                        displayName = data.displayName,
                        elo = data.elo,
                        color = CellOwner.entries[index],
                        tournamentMatchWins = tournament?.let {
                            when (data.profileId) {
                                it.leftProfileId -> it.leftWins
                                it.rightProfileId -> it.rightWins
                                else -> error("Inconsistent tournament snapshot")
                            }
                        },
                    )
                }
            }

            data class TournamentPlayer(
                val profileId: ProfileId,
                val displayName: String,
                val tournamentWins: Int,
            )

            return listOf(
                TournamentPlayer(tournament.leftProfileId, tournament.leftDisplayName, tournament.leftWins),
                TournamentPlayer(tournament.rightProfileId, tournament.rightDisplayName, tournament.rightWins),
            ).mapIndexed { index, player ->
                PlayerImpl(
                    repository = repository,
                    playerId = PlayerId(""),
                    profileId = player.profileId,
                    displayName = player.displayName,
                    elo = players.find { it.profileId == player.profileId }?.elo ?: -1,
                    color = CellOwner.entries[(index + tournament.currentGameNumber + 1) % 2],
                    tournamentMatchWins = player.tournamentWins,
                )
            }
        }

        internal fun of(
            repository: SessionRepository,
            profileRepository: ProfileRepository,
            dto: LobbyInfoDto,
        ) = LobbySessionImpl(
            repository = repository,
            id = dto.id,
            gameOptions = GameOptions(
                rated = dto.rated,
                timeControl = dto.timeControl,
                visibility = GameVisibility.Public,
            ),
            tournamentInfo = null,
            players = createPlayerList(profileRepository, dto.players, null),
            createdAt = dto.createdAt,
            startedAt = dto.startedAt,
            dto = null,
            gameState = null,
        )

        internal fun of(
            client: HdsApiClient,
            dto: SessionDto,
            gameState: SessionGameStateDto,
        ): LobbySessionImpl {
            val tournament = dto.tournament?.let { TournamentMatchSnapshotImpl.of(it, client) }
            return LobbySessionImpl(
                repository = client.sessionRepository,
                id = dto.id,
                gameOptions = dto.gameOptions,
                tournamentInfo = tournament,
                players = createPlayerList(client.profileRepository, dto.players, dto.tournament),
                createdAt = Instant.DISTANT_PAST,
                startedAt = null,
                dto = dto,
                gameState = gameState,
            )
        }
    }
}

internal class LiveSessionImpl private constructor(
    private val repository: SessionRepository,
    override val id: SessionId,
    override val gameOptions: GameOptions,
    override val tournamentInfo: TournamentMatchSnapshot?,
    override val players: List<LiveSessionPlayer>,
    override val state: SessionState.LiveSessionState,
    override val game: SessionGame,
    override val dto: SessionDto,
    internal val lastState: Pair<Instant, SessionStateDto.InGame>?,
    override val gameState: SessionGameStateDto,
) : SessionImpl(), LiveSession {
    override fun observe() = repository.observeSession(id)

    companion object {
        private fun SessionDto.createPlayerList(
            repository: ProfileRepository,
            gameState: SessionGameStateDto?,
            timeControl: TimeControl,
        ) = players.mapIndexed { index, data ->
            val owner = gameState?.playerTiles?.get(data.id)?.color?.let {
                when {
                    it.red > 200 -> CellOwner.X
                    it.blue > 200 -> CellOwner.O
                    else -> error("Unrecognized color '${it.format()}'")
                }
            } ?: CellOwner.entries[index]

            LiveSessionPlayerImpl(
                repository = repository,
                playerId = data.id,
                profileId = data.profileId?.takeIf { it.value != data.id.value },
                displayName = data.displayName,
                elo = data.rating.eloScore,
                eloAdjustment = data.ratingAdjustment?.let {
                    LiveSessionPlayer.EloAdjustment(eloGain = it.eloGain, eloLoss = it.eloLoss)
                },
                color = owner,
                tournamentMatchWins = tournament?.let {
                    when (data.profileId) {
                        it.leftProfileId -> it.leftWins
                        it.rightProfileId -> it.rightWins
                        else -> error("Inconsistent tournament snapshot")
                    }
                },
                timeRemaining = when (timeControl) {
                    is TimeControl.Unlimited -> null
                    is TimeControl.Turn ->
                        if (data.id == gameState?.currentTurnPlayerId) {
                            gameState.currentTurnExpiresIn
                        } else {
                            LiveDuration(timeControl.turnTime, Clock.System.now())
                        }
                    is TimeControl.Match ->
                        if (data.id == gameState?.currentTurnPlayerId) {
                            gameState.currentTurnExpiresIn
                        } else {
                            gameState?.playerTimeRemaining[data.id]
                        }
                },
                connectionStatus = data.connection.status,
            )
        }

        private fun SessionStateDto.GameSessionState.toSessionState(
            gameState: SessionGameStateDto?,
            lastState: Pair<Instant, SessionStateDto.InGame>?,
            playersById: Map<PlayerId, LiveSessionPlayer>,
        ) = when (this) {
            is SessionStateDto.InGame -> SessionState.InGame(
                currentTurn = SessionTurn(
                    player = playersById[gameState!!.currentTurnPlayerId]!!,
                    placementsRemaining = gameState.placementsRemaining,
                    expiresIn = gameState.currentTurnExpiresIn,
                ),
            )

            is SessionStateDto.Finished -> SessionState.Finished(
                result = GameResult(
                    winner = playersById[winningPlayerId],
                    duration = lastState?.let { it.first - it.second.startedAt } ?: Duration.ZERO,
                    reason = finishReason,
                ),
                rematchAcceptedPlayers = rematchAcceptedPlayerIds.mapNotNull { playersById[it] },
            )
        }

        internal fun of(
            client: HdsApiClient,
            dto: SessionDto,
            lastState: Pair<Instant, SessionStateDto.InGame>?,
            state: SessionStateDto.GameSessionState,
            gameState: SessionGameStateDto,
        ): LiveSessionImpl {
            val players = dto.createPlayerList(client.profileRepository, gameState, dto.gameOptions.timeControl)
            val playersById = players.associateBy { it.playerId }

            val tournament = dto.tournament?.let { TournamentMatchSnapshotImpl.of(it, client) }
            val state = state.toSessionState(gameState, lastState, playersById)

            return LiveSessionImpl(
                repository = client.sessionRepository,
                id = dto.id,
                gameOptions = dto.gameOptions,
                tournamentInfo = tournament,
                players = players,
                state = state,
                game = SessionGameImpl.of(
                    dto = dto,
                    lastState = lastState,
                    tournamentInfo = tournament,
                    gameState = gameState,
                    players = players,
                    playersById = playersById,
                    result = (state as? SessionState.Finished)?.result,
                ),
                dto = dto,
                lastState = lastState,
                gameState = gameState,
            )
        }
    }
}

private class LiveSessionPlayerImpl(
    repository: ProfileRepository,
    playerId: PlayerId,
    profileId: ProfileId?,
    displayName: String,
    elo: Int,
    override val eloAdjustment: LiveSessionPlayer.EloAdjustment?,
    color: CellOwner,
    tournamentMatchWins: Int?,
    override val timeRemaining: LiveDuration?,
    override val connectionStatus: SessionPlayerConnectionStatus,
) : LiveSessionPlayer, PlayerImpl(
    repository = repository,
    playerId = playerId,
    profileId = profileId,
    displayName = displayName,
    elo = elo,
    color = color,
    tournamentMatchWins = tournamentMatchWins,
)

private class SessionGameImpl(
    override val id: GameId,
    override val startedAt: Instant,
    override val result: GameResult?,
    override val options: GameOptions,
    override val tournamentInfo: TournamentMatchSnapshot?,
    override val moves: List<GameMove>,
    override val players: List<LiveSessionPlayer>,
) : SessionGame {
    override val moveCount get() = moves.size

    companion object {
        fun of(
            dto: SessionDto,
            lastState: Pair<Instant, SessionStateDto.InGame>?,
            tournamentInfo: TournamentMatchSnapshot?,
            gameState: SessionGameStateDto,
            players: List<LiveSessionPlayer>,
            playersById: Map<PlayerId, LiveSessionPlayer>,
            result: GameResult?,
        ) = SessionGameImpl(
            id = (dto.state as SessionStateDto.GameSessionState).gameId,
            startedAt = when (dto.state) {
                is SessionStateDto.InGame -> dto.state.startedAt
                is SessionStateDto.Finished -> lastState?.second?.startedAt ?: Instant.DISTANT_PAST
            },
            result = result,
            options = dto.gameOptions,
            tournamentInfo = tournamentInfo,
            moves = gameState.cells!!.map {
                GameMove(
                    coordinate = CellCoordinate(it.q, it.r),
                    player = playersById[it.occupiedBy]!!,
                )
            },
            players = players.sortedBy { player -> gameState.cells.indexOfFirst { it.occupiedBy == player.playerId } },
        )
    }
}
