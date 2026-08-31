package de.mineking.hexo.game.model.game

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.GamePosition
import de.mineking.hexo.board.Move
import de.mineking.hexo.game.model.EntityId
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.profile.ProfileReference
import de.mineking.hexo.game.model.tournament.TournamentMatchInfo
import de.mineking.hexo.game.model.tournament.TournamentReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Instant

@JvmInline
@Serializable
value class GameId(override val value: String) : EntityId

class GameReference(
    private val repository: FinishedGameRepository,
    val id: GameId,
) {
    suspend fun retrieve() = repository.getGame(id)
}

interface Game {
    val id: GameId
    val startedAt: Instant
    val result: GameResult?
    val options: GameOptions
    val tournament: TournamentMatchSnapshot?
    val moveCount: Int
    val players: List<Player>
}

fun Game.playerWithColor(color: CellOwner) = players.first { it.color == color }

val Game.endedAt get() = result?.let { startedAt + it.duration }

interface GameWithPosition : Game {
    val position: GamePosition<GameMove>
}

@JvmInline
@Serializable
value class PlayerId(val value: String)

interface Player {
    val id: PlayerId
    val profile: ProfileReference?
    val displayName: String
    val elo: Int
    val color: CellOwner
    val tournamentMatchWins: Int?
}

fun Player.isGuest() = profile == null

@Serializable
enum class GameVisibility {
    @SerialName("public") Public,
    @SerialName("private") Private,
}

interface GameOptions {
    val rated: Boolean
    val visibility: GameVisibility
    val timeControl: TimeControl
}

sealed interface GameFinishReason {
    data class Regular(val length: Int) : GameFinishReason

    data object Timeout : GameFinishReason
    data object Surrender : GameFinishReason
    data object Disconnect : GameFinishReason
    data object DrawAgreement : GameFinishReason
    data object Terminated : GameFinishReason
}

data class GameResult(
    val winner: Player?,
    val duration: Duration,
    val reason: GameFinishReason,
)

open class GameMove(
    override val coordinate: CellCoordinate,
    val player: Player,
) : Move {
    override val owner get() = player.color
}

class TournamentMatchSnapshot(
    val tournament: TournamentReference,
    val matchInfo: TournamentMatchInfo,
)
