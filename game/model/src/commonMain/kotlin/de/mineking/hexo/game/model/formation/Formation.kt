package de.mineking.hexo.game.model.formation

import de.mineking.hexo.board.GamePosition
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class FormationId(val value: String)

interface Formation {
    val id: FormationId
    val url: String
    val name: String

    val position: GamePosition<*>
}
