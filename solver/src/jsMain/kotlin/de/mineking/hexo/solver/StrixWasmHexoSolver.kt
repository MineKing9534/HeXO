package de.mineking.hexo.solver

import cc.tyto.CoordW
import cc.tyto.DefenseKind
import cc.tyto.DefenseOutcome
import cc.tyto.Player
import cc.tyto.Position
import cc.tyto.SolveKind
import cc.tyto.SolveOutcome
import cc.tyto.SolverEngine
import cc.tyto.SolverLimits
import cc.tyto.StrixSolver
import cc.tyto.init
import cc.tyto.jsBigInt
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.core.CellOwner
import kotlinx.coroutines.await
import org.khronos.webgl.Int32Array
import org.khronos.webgl.set

class StrixWasmHexoSolver : HexoSolver {
    companion object {
        @OptIn(ExperimentalWasmJsInterop::class)
        // lazy so this is only called once the wasm backend is initialized
        private val limits by lazy {
            SolverLimits(
                depthCap = 10,
                nodeBudget = jsBigInt(20_000),
                engine = SolverEngine.Idtt,
            )
        }
    }

    private val ready = init()

    // lazy so this is only called once the wasm backend is initialized
    private val solver by lazy { StrixSolver() }

    override suspend fun findWin(board: Board, player: CellOwner, remaining: Int): FindWinResult {
        ready.await()

        val position = board.toPosition(player, remaining)
        val outcome = solver.solveWide(position, limits)

        return outcome.toResult()
    }

    override suspend fun findDefense(board: Board, player: CellOwner, remaining: Int): FindDefenseResult {
        ready.await()

        val position = board.toPosition(player, remaining)
        val outcome = solver.solveDefense(position, limits)

        return outcome.toResult()
    }

    private fun Map<CellCoordinate, Cell>.toFlatStones(): Int32Array {
        val stones = flatMap { (coordinate, cell) ->
            val owner = cell.owner ?: return@flatMap emptyList()
            listOf(coordinate.q, coordinate.r, owner.strixValue)
        }

        val result = Int32Array(stones.size)
        stones.forEachIndexed { index, value -> result[index] = value }

        return result
    }

    private fun Board.toPosition(toMove: CellOwner, remaining: Int) = Position(
        winLength = 6,
        placementRadius = 8,
        maxMoves = 300,
        toMove = toMove.strix,
        movesRemaining = remaining,
        stonesFlat = cells.toFlatStones(),
    )

    private fun SolveOutcome.toResult() = when (kind) {
        SolveKind.BudgetExceeded -> FindWinResult.Unknown
        SolveKind.No -> FindWinResult.NoWin
        SolveKind.Win -> FindWinResult.Win(
            turns = pv.map { turn ->
                Turn(
                    player = turn.player.core,
                    cells = turn.cells.map { it.core },
                )
            },
        )
    }

    private fun DefenseOutcome.toResult() = when (kind) {
        DefenseKind.NoThreat -> FindDefenseResult.NoThreat
        DefenseKind.ThreatFound -> FindDefenseResult.Threat(
            threat = threat!!.toResult() as FindWinResult.Win,
            defenses = killers.takeIf { it.isNotEmpty() }
                ?.map { Defense(it.core, null) }
                ?: pairAnchors.map { (first, second) -> Defense(first.core, second.core) },
            bestDelay = bestDelay?.core,
        )
    }
}

private val CoordW.core get() = CellCoordinate(q, r)

private val CellOwner.strixValue get() = ordinal + 1
private val CellOwner.strix get() = when (this) {
    CellOwner.X -> Player.P1
    CellOwner.O -> Player.P2
}

private val Player.core get() = when (this) {
    Player.P1 -> CellOwner.X
    Player.P2 -> CellOwner.O
}
