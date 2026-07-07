package de.mineking.hexo.hds.session

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.game.Game
import de.mineking.hexo.hds.game.GameId
import de.mineking.hexo.hds.game.GameMove
import de.mineking.hexo.hds.game.GameOptions
import de.mineking.hexo.hds.game.GameResult
import de.mineking.hexo.hds.game.GameVisibility
import de.mineking.hexo.hds.game.Player
import de.mineking.hexo.hds.game.PlayerId
import de.mineking.hexo.hds.game.TournamentMatchSnapshot
import de.mineking.hexo.hds.profile.ProfileId
import de.mineking.hexo.hds.profile.ProfileRepository
import de.mineking.hexo.hds.utils.EntityState
import de.mineking.hexo.hds.utils.LiveDuration
import de.mineking.hexo.hds.utils.TimeControl
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Clock
import kotlin.time.Instant

@JvmInline
@Serializable
value class SessionId(val value: String)

class SessionReference(
    private val repository: SessionRepository,
    val id: SessionId,
) {
    fun observe() = repository.observeSession(id)
}

interface SessionPlayer {
    val profileId: ProfileId?
    val displayName: String
    val elo: Int
}

fun SessionPlayer.isGuest() = profileId != null

sealed interface SessionState {
    data object Lobby : SessionState

    sealed interface LiveSessionState : SessionState

    data class InGame(val currentTurn: SessionTurn) : LiveSessionState
    data class Finished(val result: GameResult, val rematchAcceptedPlayers: List<LiveSessionPlayer>) : LiveSessionState
}

abstract class Session(
    val id: SessionId,
    val gameOptions: GameOptions,
    val tournamentInfo: TournamentMatchSnapshot?,
) {
    abstract val players: List<SessionPlayer>
    abstract val state: SessionState
    internal abstract val dto: SessionDto?
    internal abstract val gameState: SessionGameStateDto?

    abstract fun observe(): SharedFlow<EntityState<Session>>

    companion object {
        internal fun of(
            client: HdsApiClient,
            dto: SessionDto,
            gameState: SessionGameStateDto,
        ) = when (dto.state) {
            is SessionStateDto.Lobby -> LobbySession.of(client, dto, dto.state, gameState)
            is SessionStateDto.GameSessionState -> LiveSession.of(client, dto, dto.state, gameState)
        }
    }
}

class LobbySession private constructor(
    private val repository: SessionRepository,
    id: SessionId,
    gameOptions: GameOptions,
    tournamentInfo: TournamentMatchSnapshot?,
    override val players: List<SessionPlayer>,
    val createdAt: Instant,
    val startedAt: Instant?,
    override val dto: SessionDto?,
    override val gameState: SessionGameStateDto?,
) : Session(id, gameOptions, tournamentInfo) {
    override val state = SessionState.Lobby

    override fun observe() = repository.observeSession(id)

    companion object {
        internal fun of(repository: SessionRepository, dto: LobbyInfoDto) = LobbySession(
            repository = repository,
            id = dto.id,
            gameOptions = GameOptions(
                rated = dto.rated,
                timeControl = dto.timeControl,
                visibility = GameVisibility.Public,
            ),
            tournamentInfo = null,
            players = dto.players,
            createdAt = dto.createdAt,
            startedAt = dto.startedAt,
            dto = null,
            gameState = null,
        )

        internal fun of(
            client: HdsApiClient,
            dto: SessionDto,
            state: SessionStateDto.Lobby,
            gameState: SessionGameStateDto,
        ): LobbySession {
            val tournament = dto.tournament?.let { TournamentMatchSnapshot.of(it, client) }
            return LobbySession(
                repository = client.sessionRepository,
                id = dto.id,
                gameOptions = dto.gameOptions,
                tournamentInfo = tournament,
                players = dto.players,
                createdAt = state.createdAt,
                startedAt = null,
                dto = dto,
                gameState = gameState,
            )
        }
    }
}

fun LobbySession.hasStarted() = startedAt != null

class LiveSession private constructor(
    private val repository: SessionRepository,
    id: SessionId,
    gameOptions: GameOptions,
    tournamentInfo: TournamentMatchSnapshot?,
    override val players: List<LiveSessionPlayer>,
    override val state: SessionState.LiveSessionState,
    val game: SessionGame,
    override val dto: SessionDto,
    override val gameState: SessionGameStateDto,
) : Session(id, gameOptions, tournamentInfo) {
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

            LiveSessionPlayer(
                repository = repository,
                playerId = data.id,
                profileId = data.profileId?.takeIf { it.value != data.id.value },
                displayName = data.displayName,
                elo = data.rating.eloScore,
                eloAdjustment = data.ratingAdjustment?.let {
                    SessionPlayerEloAdjustment(eloGain = it.eloGain, eloLoss = it.eloLoss)
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
                    duration = finishedAt - startedAt,
                    reason = finishReason,
                ),
                rematchAcceptedPlayers = rematchAcceptedPlayerIds.mapNotNull { playersById[it] },
            )
        }

        internal fun of(
            client: HdsApiClient,
            dto: SessionDto,
            state: SessionStateDto.GameSessionState,
            gameState: SessionGameStateDto,
        ): LiveSession {
            val players = dto.createPlayerList(client.profileRepository, gameState, dto.gameOptions.timeControl)
            val playersById = players.associateBy { it.playerId }

            val tournament = dto.tournament?.let { TournamentMatchSnapshot.of(it, client) }
            val state = state.toSessionState(gameState, playersById)

            return LiveSession(
                repository = client.sessionRepository,
                id = dto.id,
                gameOptions = dto.gameOptions,
                tournamentInfo = tournament,
                players = players,
                state = state,
                game = SessionGame.of(
                    dto = dto,
                    tournamentInfo = tournament,
                    gameState = gameState,
                    players = players,
                    playersById = playersById,
                    result = (state as? SessionState.Finished)?.result,
                ),
                dto = dto,
                gameState = gameState,
            )
        }
    }
}

enum class SessionPlayerConnectionStatus {
    Connected,
    Orphaned,
    Disconnected,
}

data class SessionPlayerEloAdjustment(
    val eloGain: Int,
    val eloLoss: Int,
)

class LiveSessionPlayer(
    repository: ProfileRepository,
    playerId: PlayerId,
    profileId: ProfileId?,
    displayName: String,
    elo: Int,
    val eloAdjustment: SessionPlayerEloAdjustment?,
    color: CellOwner,
    tournamentMatchWins: Int?,
    val timeRemaining: LiveDuration?,
    val connectionStatus: SessionPlayerConnectionStatus,
) : SessionPlayer, Player(
    repository = repository,
    playerId = playerId,
    profileId = profileId,
    displayName = displayName,
    elo = elo,
    color = color,
    tournamentMatchWins = tournamentMatchWins,
) {
    override val profileId = super.profileId
    override val displayName = super.displayName
    override val elo = super.elo
}

data class SessionTurn(
    val player: LiveSessionPlayer,
    val placementsRemaining: Int,
    val expiresIn: LiveDuration?,
)

class SessionGame(
    override val id: GameId,
    override val startedAt: Instant,
    override val result: GameResult?,
    override val options: GameOptions,
    override val tournamentInfo: TournamentMatchSnapshot?,
    override val moves: List<GameMove>,
    override val players: List<LiveSessionPlayer>,
) : Game {
    override val moveCount get() = moves.size

    companion object {
        internal fun of(
            dto: SessionDto,
            tournamentInfo: TournamentMatchSnapshot?,
            gameState: SessionGameStateDto,
            players: List<LiveSessionPlayer>,
            playersById: Map<PlayerId, LiveSessionPlayer>,
            result: GameResult?,
        ) = SessionGame(
            id = (dto.state as SessionStateDto.GameSessionState).gameId,
            startedAt = when (dto.state) {
                is SessionStateDto.InGame -> dto.state.startedAt
                is SessionStateDto.Finished -> dto.state.startedAt
            },
            result = result,
            options = dto.gameOptions,
            tournamentInfo = tournamentInfo,
            moves = gameState.cells?.map {
                GameMove(
                    coordinate = CellCoordinate(it.q, it.r),
                    player = playersById[it.occupiedBy]!!,
                )
            }!!,
            players = players,
        )
    }
}
