package de.mineking.hexo.game.model.session

import de.mineking.hexo.board.moves
import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.game.model.LiveDuration
import de.mineking.hexo.game.model.game.GameOptions
import de.mineking.hexo.game.model.game.GameResult
import de.mineking.hexo.game.model.game.GameWithPosition
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.game.TournamentMatchSnapshot
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
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

sealed interface SessionState {
    data object Lobby : SessionState

    sealed interface LiveSessionState : SessionState
    data class InGame(val currentTurn: SessionTurn) : LiveSessionState
    data class Finished(val result: GameResult, val rematchAcceptedPlayers: List<LiveSessionPlayer>) : LiveSessionState
}

interface Session {
    val id: SessionId
    val gameOptions: GameOptions
    val tournament: TournamentMatchSnapshot?

    val players: List<Player>
    val state: SessionState

    val createdAt: Instant
    val startedAt: Instant?

    fun observe(): StateFlow<EntityState<Session>>
}

fun Session.hasStarted() = startedAt != null

interface LobbySession : Session {
    override val startedAt: Nothing? get() = null
}

interface LiveSession : Session {
    override val players: List<LiveSessionPlayer>
    override val state: SessionState.LiveSessionState

    override val startedAt: Instant

    val game: SessionGame
}

enum class SessionPlayerConnectionStatus {
    Connected,
    Orphaned,
    Disconnected,
}

data class RatingAdjustment(
    val eloGain: Int,
    val eloLoss: Int,
)

interface LiveSessionPlayer : Player {
    val ratingAdjustment: RatingAdjustment?
    val timeRemaining: LiveDuration?
    val connectionStatus: SessionPlayerConnectionStatus
}

data class SessionTurn(
    val player: LiveSessionPlayer,
    val placementsRemaining: Int,
    val expiresIn: LiveDuration?,
)

interface SessionGame : GameWithPosition {
    override val players: List<LiveSessionPlayer>

    override val moveCount get() = position.moves.size
}
