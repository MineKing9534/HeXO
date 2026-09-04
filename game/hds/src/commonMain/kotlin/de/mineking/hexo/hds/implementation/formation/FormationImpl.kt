package de.mineking.hexo.hds.implementation.formation

import de.mineking.hexo.board.toGamePosition
import de.mineking.hexo.game.model.formation.Formation
import de.mineking.hexo.game.model.urlOf
import de.mineking.hexo.hds.implementation.HdsApiClient

internal class FormationImpl(
    client: HdsApiClient,
    dto: FormationDto,
) : Formation {
    override val id = dto.id
    override val url = client.formationRepository.urlOf(id)
    override val name = dto.name
    override val position = dto.gamePosition.cells.toGamePosition()
}
