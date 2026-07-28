package de.mineking.hexo.hds.game

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.hds.AbstractGamePosition
import de.mineking.hexo.hds.HdsApiClient
import de.mineking.hexo.hds.profile.ProfileId
import de.mineking.hexo.hds.profile.ProfileRepository
import de.mineking.hexo.hds.tournament.TournamentBracket
import de.mineking.hexo.hds.tournament.TournamentId
import de.mineking.hexo.hds.tournament.TournamentMatchId
import de.mineking.hexo.hds.tournament.TournamentRepository
import de.mineking.hexo.hds.utils.Duration
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
@Serializable
value class PlayerId(val value: String)

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

class FinishedGame(
    override val id: GameId,
    override val startedAt: Instant,
    val url: String,
    override val result: GameResult,
    override val options: GameOptions,
    override val tournamentInfo: TournamentMatchSnapshot?,
    override val moves: List<GameMove>,
    override val moveCount: Int,
    override val players: List<FinishedGamePlayer>,
) : Game {
    companion object {
        private fun FinishedGameDto.createPlayerList(repository: ProfileRepository) = players.map { data ->
            val color = playerTiles[data.playerId]?.color ?: error("Player tile for ${data.playerId} not defined")
            val owner = when {
                color.red > 200 -> CellOwner.X
                color.blue > 200 -> CellOwner.O
                else -> error("Unrecognized color '${color.format()}'")
            }

            FinishedGamePlayer(
                repository = repository,
                playerId = data.playerId,
                profileId = data.profileId.takeIf { it.value != data.playerId.value },
                displayName = data.displayName,
                elo = data.elo,
                eloChange = data.eloChange,
                color = owner,
                tournamentMatchWins = tournament?.let {
                    when (data.profileId) {
                        it.leftProfileId -> it.leftWins
                        it.rightProfileId -> it.rightWins
                        else -> error("Inconsistent tournament snapshot")
                    }
                },
            )
        }.sortedBy { player -> moves.indexOfFirst { it.playerId == player.playerId } }

        internal fun of(
            client: HdsApiClient,
            dto: FinishedGameDto,
        ): FinishedGame {
            val players = dto.createPlayerList(client.profileRepository)
            val playersById = players.associateBy { it.playerId }

            return FinishedGame(
                id = dto.id,
                startedAt = dto.startedAt,
                url = "${client.host}/games/${dto.id.value}",
                result = GameResult(playersById[dto.result.winningPlayerId], dto.result.duration, dto.result.reason),
                options = dto.options,
                tournamentInfo = dto.tournament?.let { TournamentMatchSnapshot.of(it, client) },
                moves = dto.moves.map {
                    GameMove(
                        coordinate = CellCoordinate(it.q, it.r),
                        player = playersById[it.playerId]!!,
                    )
                },
                moveCount = dto.moveCount,
                players = players,
            )
        }
    }
}

class FinishedGamePlayer(
    repository: ProfileRepository,
    playerId: PlayerId,
    profileId: ProfileId?,
    displayName: String,
    elo: Int,
    val eloChange: Int?,
    color: CellOwner,
    tournamentMatchWins: Int?,
) : Player(
    repository = repository,
    playerId = playerId,
    profileId = profileId,
    displayName = displayName,
    elo = elo,
    color = color,
    tournamentMatchWins = tournamentMatchWins,
)

open class Player(
    private val repository: ProfileRepository,
    val playerId: PlayerId,
    open val profileId: ProfileId?,
    open val displayName: String,
    open val elo: Int,
    val color: CellOwner,
    val tournamentMatchWins: Int?,
) {
    suspend fun fetchProfile() = profileId?.let { repository.getProfile(it) }
}

fun Player.isGuest() = profileId == null

class TournamentMatchSnapshot(
    private val repository: TournamentRepository,
    val tournamentId: TournamentId,
    val tournamentUrl: String,
    val tournamentName: String,
    val matchId: TournamentMatchId,
    val bracket: TournamentBracket,
    val round: Int,
    val order: Int,
    val bestOf: Int,
    val currentGameNumber: Int,
) {
    suspend fun retrieveTournament() = repository.getTournament(tournamentId)
    fun observeTournament() = repository.observeTournament(tournamentId)

    companion object {
        internal fun of(
            dto: TournamentMatchSnapshotDto,
            client: HdsApiClient,
        ) = TournamentMatchSnapshot(
            repository = client.tournamentRepository,
            tournamentId = dto.tournamentId,
            tournamentUrl = "${client.host}/tournaments/${dto.tournamentId.value}",
            tournamentName = dto.tournamentName,
            matchId = dto.matchId,
            bracket = dto.bracket,
            round = dto.round,
            order = dto.order,
            bestOf = dto.bestOf,
            currentGameNumber = dto.currentGameNumber,
        )
    }
}

data class GameResult(
    val winner: Player?,
    val duration: Duration,
    val reason: GameFinishReason,
)

interface Move {
    val coordinate: CellCoordinate
    val owner: CellOwner
}

data class GameMove(
    override val coordinate: CellCoordinate,
    val player: Player,
) : Move {
    override val owner = player.color
}
