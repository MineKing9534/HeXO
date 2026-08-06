package de.mineking.hexo.hds.implementation.formation

import de.mineking.hexo.hds.model.formation.Formation
import de.mineking.hexo.hds.model.formation.FormationId
import de.mineking.hexo.hds.model.formation.GamePosition

internal class FormationImpl(
    override val id: FormationId,
    override val url: String,
    override val name: String,
    override val gamePosition: GamePosition,
) : Formation {
    companion object {
        internal fun of(host: String, dto: FormationDto): Formation {
            return FormationImpl(
                id = dto.id,
                url = "$host/sandbox/${dto.id.value}",
                name = dto.name,
                gamePosition = dto.gamePosition,
            )
        }
    }
}
