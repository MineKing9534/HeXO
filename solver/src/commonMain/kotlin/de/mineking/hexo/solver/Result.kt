package de.mineking.hexo.solver

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import kotlinx.serialization.Serializable

@Serializable
data class Turn(val player: CellOwner, val cells: List<CellCoordinate>)

@Serializable
sealed interface FindWinResult {
    @Serializable
    data object NoWin : FindWinResult

    @Serializable
    data object Unknown : FindWinResult

    @Serializable
    data class Win(val turns: List<Turn>) : FindWinResult
}

@Serializable
data class Defense(val first: CellCoordinate, val second: CellCoordinate?) : Collection<CellCoordinate> by listOfNotNull(first, second)

@Serializable
sealed interface FindDefenseResult {
    @Serializable
    data object NoThreat : FindDefenseResult

    @Serializable
    data class Threat(
        val threat: FindWinResult.Win,
        val defenses: List<Defense>,
        val bestDelay: CellCoordinate?,
    ) : FindDefenseResult
}

fun FindDefenseResult.isDefendable() = this !is FindDefenseResult.Threat || this.defenses.isNotEmpty()
fun FindDefenseResult.isLost() = this is FindDefenseResult.Threat && this.defenses.isEmpty()
