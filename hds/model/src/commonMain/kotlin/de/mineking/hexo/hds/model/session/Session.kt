package de.mineking.hexo.hds.model.session

import de.mineking.hexo.hds.model.EntityState
import de.mineking.hexo.hds.model.LiveDuration
import de.mineking.hexo.hds.model.game.Game
import de.mineking.hexo.hds.model.game.GameOptions
import de.mineking.hexo.hds.model.game.GameResult
import de.mineking.hexo.hds.model.game.Player
import de.mineking.hexo.hds.model.game.TournamentMatchSnapshot
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
    val tournamentInfo: TournamentMatchSnapshot?

    val players: List<Player>
    val state: SessionState

    fun observe(): StateFlow<EntityState<Session>>
}

interface LobbySession : Session {
    val createdAt: Instant
    val startedAt: Instant?
}

fun LobbySession.hasStarted() = startedAt != null

interface LiveSession : Session {
    override val players: List<LiveSessionPlayer>
    override val state: SessionState.LiveSessionState
    val game: SessionGame
}

enum class SessionPlayerConnectionStatus {
    Connected,
    Orphaned,
    Disconnected,
}

interface LiveSessionPlayer : Player {
    val eloAdjustment: EloAdjustment?
    val timeRemaining: LiveDuration?
    val connectionStatus: SessionPlayerConnectionStatus

    data class EloAdjustment(
        val eloGain: Int,
        val eloLoss: Int,
    )
}

data class SessionTurn(
    val player: LiveSessionPlayer,
    val placementsRemaining: Int,
    val expiresIn: LiveDuration?,
)

interface SessionGame : Game {
    override val players: List<LiveSessionPlayer>

    override val moveCount get() = moves.size
}
