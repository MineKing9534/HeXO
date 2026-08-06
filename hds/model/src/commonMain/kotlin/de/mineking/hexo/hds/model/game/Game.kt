package de.mineking.hexo.hds.model.game

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.hds.model.AbstractGamePosition
import de.mineking.hexo.hds.model.Duration
import de.mineking.hexo.hds.model.EntityState
import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.Move
import de.mineking.hexo.hds.model.TimeControl
import de.mineking.hexo.hds.model.profile.Profile
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.tournament.Tournament
import de.mineking.hexo.hds.model.tournament.TournamentBracket
import de.mineking.hexo.hds.model.tournament.TournamentId
import de.mineking.hexo.hds.model.tournament.TournamentMatchId
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class GameId(val value: String)

class GameReference(
    private val repository: FinishedGameRepository,
    val id: GameId,
) {
    suspend fun retrieveGame() = repository.getGame(id)
}

interface Game : AbstractGamePosition {
    val id: GameId
    val startedAt: Instant
    val result: GameResult?
    val options: GameOptions
    val tournamentInfo: TournamentMatchSnapshot?
    override val moves: List<GameMove>
    val moveCount: Int
    val players: List<Player>
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

@Serializable
data class GameOptions(
    val rated: Boolean,
    val visibility: GameVisibility,
    val timeControl: TimeControl,
)

@Serializable
enum class GameFinishReason {
    @SerialName("six-in-a-row") SixInARow,
    @SerialName("timeout") Timeout,
    @SerialName("surrender") Surrender,
    @SerialName("disconnect") Disconnect,
    @SerialName("draw-agreement") DrawAgreement,
    @SerialName("terminated") Terminated,
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

interface TournamentMatchSnapshot {
    val tournamentId: TournamentId
    val tournamentUrl: String
    val tournamentName: String
    val matchId: TournamentMatchId
    val bracket: TournamentBracket
    val round: Int
    val order: Int
    val bestOf: Int
    val currentGameNumber: Int

    suspend fun retrieveTournament(): Tournament?
    fun observeTournament(): StateFlow<EntityState<Tournament>>
}
