package de.mineking.hexo.game.model.session

import de.mineking.hexo.board.moves
import de.mineking.hexo.game.model.Entity
import de.mineking.hexo.game.model.EntityId
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
value class SessionId(override val value: String) : EntityId

class SessionReference(
    private val repository: SessionRepository,
    val id: SessionId,
) {
    fun observe() = repository.observeSession(id)
}

sealed interface SessionState {
    sealed interface Live : Detailed

    sealed interface Detailed : SessionState {
        data class InGame(val currentTurn: SessionTurn) : SessionState.InGame, Live
        data class Finished(val result: GameResult, val rematchAcceptedPlayers: List<LiveSessionPlayer>) : SessionState.Finished, Live
    }

    data object Lobby : Detailed

    sealed interface InGame : SessionState {
        companion object : InGame
    }

    sealed interface Finished : SessionState {
        companion object : Finished
    }
}

interface Session : Entity<SessionId> {
    override val id: SessionId
    override val url: String
    val gameOptions: GameOptions
    val tournament: TournamentMatchSnapshot?

    val players: List<Player>
    val state: SessionState

    val createdAt: Instant
    val startedAt: Instant?

    fun observe(): StateFlow<EntityState<Session>>
}

fun Session.hasStarted() = startedAt != null

interface DetailedSession : Session {
    override val state: SessionState.Detailed
}

interface LobbySession : DetailedSession {
    override val state: SessionState.Lobby get() = SessionState.Lobby
    override val startedAt: Nothing? get() = null
}

interface LiveSession : DetailedSession {
    override val players: List<LiveSessionPlayer>
    override val state: SessionState.Live

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
