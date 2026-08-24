package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.toGamePosition
import de.mineking.hexo.game.model.game.FinishedGameMove
import de.mineking.hexo.game.model.game.FinishedGamePlayer
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameResult
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.game.PlayerId
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.hds.implementation.HdsApiClient

internal class FinishedGameImpl(
    private val client: HdsApiClient,
    private val dto: FinishedGameDto,
) : FinishedGameWithPosition {
    private fun getPlayerById(id: PlayerId) = players.first { it.playerId == id }

    override val players = dto.players.map { data ->
        val color = dto.playerTiles[data.playerId]?.color ?: error("Player tile for ${data.playerId} not defined")
        val owner = when {
            color.red > 200 -> CellOwner.X
            color.blue > 200 -> CellOwner.O
            else -> error("Unrecognized color '${color.format()}'")
        }

        FinishedGamePlayerImpl(
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
        )
    }.sortedBy { player -> dto.moves.indexOfFirst { it.playerId == player.playerId } }

    override val position = dto.moves.map {
        FinishedGameMove(
            coordinate = CellCoordinate(it.q, it.r),
            player = getPlayerById(it.playerId),
            it.timestamp,
        )
    }.toGamePosition()

    override val id = dto.id
    override val startedAt = dto.startedAt
    override val url = "${client.host}/games/${dto.id.value}"
    override val result = GameResult(
        winner = dto.result.winningPlayerId?.let { getPlayerById(it) },
        duration = dto.result.duration,
        reason = dto.result.reason.model,
    )
    override val options = dto.options
    override val tournament = dto.tournament?.toModel(client)
    override val moveCount = dto.moveCount
}

internal abstract class PlayerImpl(
    private val repository: ProfileRepository,
) : Player {
    override suspend fun fetchProfile() = profileId?.let { repository.getProfile(it) }
}

internal class FinishedGamePlayerImpl(
    private val client: HdsApiClient,
    private val dto: PlayerDto,
    override val color: CellOwner,
    override val tournamentMatchWins: Int?,
) : FinishedGamePlayer, PlayerImpl(client.profileRepository) {
    override val playerId = dto.playerId
    override val profileId = dto.profileId
    override val displayName = dto.displayName
    override val elo = dto.elo
    override val eloChange = dto.eloChange
}
