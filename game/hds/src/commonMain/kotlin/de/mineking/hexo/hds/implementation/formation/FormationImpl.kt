package de.mineking.hexo.hds.implementation.formation

import de.mineking.hexo.board.GamePosition
import de.mineking.hexo.board.NextTurnMetaData
import de.mineking.hexo.board.toTurns
import de.mineking.hexo.game.model.formation.Formation
import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.types.urlOf

internal class FormationImpl(
    client: HdsApiClient,
    dto: FormationDto,
) : Formation {
    override val id = dto.id
    override val url = client.formationRepository.urlOf(id)
    override val name = dto.name
    override val position = dto.gamePosition.toGamePosition()
}

private fun GamePositionDto.toGamePosition(): GamePosition<GamePositionCell> {
    val turns = cells.toTurns()
    val lastTurn = turns.lastOrNull()?.meta?.turn ?: -1

    return GamePosition(
        turns = turns,
        nextTurn = NextTurnMetaData(
            player = currentTurnPlayer,
            turn = lastTurn + (if (turns.lastOrNull()?.meta?.player == currentTurnPlayer) 0 else 1),
            placementsRemaining = placementsRemaining,
        ),
    )
}
