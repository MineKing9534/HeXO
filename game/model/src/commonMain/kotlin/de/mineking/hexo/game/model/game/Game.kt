package de.mineking.hexo.game.model.game

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.GamePosition
import de.mineking.hexo.board.Move
import de.mineking.hexo.game.model.EntityId
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.tournament.TournamentInfo
import de.mineking.hexo.game.model.tournament.TournamentMatchInfo
import de.mineking.hexo.game.model.tournament.TournamentRepository
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
    suspend fun retrieveGame() = repository.getGame(id)
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
    val playerId: PlayerId
    val profileId: ProfileId?
    val displayName: String
    val elo: Int
    val color: CellOwner
    val tournamentMatchWins: Int?

    suspend fun fetchProfile(): Profile?
}

fun Player.isGuest() = profileId == null

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
    private val repository: TournamentRepository,
    val tournamentInfo: TournamentInfo,
    val matchInfo: TournamentMatchInfo,
) {
    suspend fun retrieveTournament() = repository.getTournament(tournamentInfo.id)
    fun observeTournament() = repository.observeTournament(tournamentInfo.id)
}
