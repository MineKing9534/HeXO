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
data class PartialTurn(
    val first: CellCoordinate,
    val second: CellCoordinate?,
    val isCounterThreat: Boolean,
) : Collection<CellCoordinate> by listOfNotNull(first, second)

@Serializable
sealed interface FindDefenseResult {
    @Serializable
    data object NoThreat : FindDefenseResult

    @Serializable
    data class Threat(
        val threat: FindWinResult.Win,
        val defense: DefenseResult,
    ) : FindDefenseResult

    @Serializable
    data object Unknown : FindDefenseResult
}

@Serializable
sealed interface DefenseResult {
    @Serializable
    data class Found(
        val defenses: List<PartialTurn>,
    ) : DefenseResult

    @Serializable
    data class BudgetExceeded(
        val tacticalMoves: List<PartialTurn>,
    ) : DefenseResult

    @Serializable
    data class Undefendable(
        val bestDelay: PartialTurn?,
    ) : DefenseResult
}

fun FindDefenseResult.isDefendable() = this !is FindDefenseResult.Threat || this.defense is DefenseResult.Found
fun FindDefenseResult.isLost() = this is FindDefenseResult.Threat && this.defense is DefenseResult.Undefendable
