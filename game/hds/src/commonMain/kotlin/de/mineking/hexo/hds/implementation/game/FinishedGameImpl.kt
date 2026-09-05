package de.mineking.hexo.hds.implementation.game

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.toGamePosition
import de.mineking.hexo.game.model.EntityNotFoundException
import de.mineking.hexo.game.model.game.FinishedGameMove
import de.mineking.hexo.game.model.game.FinishedGamePlayer
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameResult
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.game.PlayerId
import de.mineking.hexo.game.model.profile.ProfileReference
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.game.model.urlOf
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.types.orThrow

internal class FinishedGameImpl(
    private val client: HdsApiClient,
    private val dto: FinishedGameDto,
) : FinishedGameWithPosition {
    private fun getPlayerById(id: PlayerId) = players.first { it.id == id }

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
    }.sortedBy { player -> dto.moves.indexOfFirst { it.playerId == player.id } }

    override val position = dto.moves.map {
        FinishedGameMove(
            coordinate = CellCoordinate(it.q, it.r),
            player = getPlayerById(it.playerId),
            it.timestamp,
        )
    }.toGamePosition()

    override val id = dto.id
    override val startedAt = dto.startedAt
    override val url = client.finishedGameRepository.urlOf(id)
    override val result = GameResult(
        winner = dto.result.winningPlayerId?.let { getPlayerById(it) },
        duration = dto.result.duration,
        reason = dto.result.reason.model,
    )
    override val options = dto.options
    override val tournament = dto.tournament?.toModel(client)
    override val moveCount = dto.moveCount

    override suspend fun withPosition() = client.finishedGameRepository.getGame(id)
        .orThrow { EntityNotFoundException() }
}

internal abstract class PlayerImpl(
    private val repository: ProfileRepository,
    private val gameRepository: FinishedGameRepository,
    dto: AbstractPlayerDto,
) : Player {
    override val id = dto.playerId
    override val profile = dto.profileId
        ?.takeIf { it.value != id.value }
        ?.let { ProfileReference(repository, gameRepository, it) }

    override val displayName = dto.displayName
    override val elo = dto.elo
}

internal class FinishedGamePlayerImpl(
    client: HdsApiClient,
    dto: PlayerDto,
    override val color: CellOwner,
    override val tournamentMatchWins: Int?,
) : FinishedGamePlayer, PlayerImpl(client.profileRepository, client.finishedGameRepository, dto) {
    override val eloChange = dto.eloChange
}
