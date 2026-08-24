package de.mineking.hexo.hds.implementation.formation

import de.mineking.hexo.board.toGamePosition
import de.mineking.hexo.game.model.formation.Formation
import de.mineking.hexo.hds.implementation.HdsApiClient

internal class FormationImpl(
    private val client: HdsApiClient,
    private val dto: FormationDto,
) : Formation {
    override val id = dto.id
    override val url = "${client.host}/sandbox/${dto.id.value}"
    override val name = dto.name
    override val position = dto.gamePosition.cells.toGamePosition()
}
