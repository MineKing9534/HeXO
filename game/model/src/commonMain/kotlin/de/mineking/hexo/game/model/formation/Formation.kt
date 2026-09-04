package de.mineking.hexo.game.model.formation

import de.mineking.hexo.board.GamePosition
import de.mineking.hexo.game.model.Entity
import de.mineking.hexo.game.model.EntityId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class FormationId(override val value: String) : EntityId

interface Formation : Entity<FormationId> {
    override val id: FormationId
    override val url: String
    val name: String

    val position: GamePosition<*>
}
