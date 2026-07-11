package de.mineking.hexo.solver

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.core.CellOwner

data class Turn(val player: CellOwner, val cells: List<CellCoordinate>)

sealed interface FindWinResult {
    data object NoWin : FindWinResult
    data object Unknown : FindWinResult
    data class Win(val turns: List<Turn>) : FindWinResult
}

data class Defense(val first: CellCoordinate, val second: CellCoordinate?) : Collection<CellCoordinate> by listOfNotNull(first, second)

sealed interface FindDefenseResult {
    data object NoThreat : FindDefenseResult
    data class Threat(
        val threat: FindWinResult.Win,
        val defenses: List<Defense>,
        val bestDelay: CellCoordinate?,
    ) : FindDefenseResult
}

fun FindDefenseResult.isDefendable() = this !is FindDefenseResult.Threat || this.defenses.isNotEmpty()
fun FindDefenseResult.isLost() = this is FindDefenseResult.Threat && this.defenses.isEmpty()
