package de.mineking.hexo.hds.model.formation

import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.hds.model.AbstractGamePosition
import de.mineking.hexo.hds.model.Move
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class FormationId(val value: String)

interface Formation : AbstractGamePosition {
    override val moves get() = gamePosition.moves

    val id: FormationId
    val url: String
    val name: String
    val gamePosition: GamePosition
}

interface GamePosition {
    val currentTurnPlayer: CellOwner
    val placementsRemaining: Int
    val moves: List<Move>
}
