package de.mineking.hexo.solver.strix

import cc.tyto.CoordW
import cc.tyto.DefenseKind
import cc.tyto.DefenseOutcome
import cc.tyto.PairAnchor
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
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.solver.BoardTransformResult
import de.mineking.hexo.solver.DefenseResult
import de.mineking.hexo.solver.FindDefenseResult
import de.mineking.hexo.solver.FindWinResult
import de.mineking.hexo.solver.HexoSolver
import de.mineking.hexo.solver.PartialTurn
import de.mineking.hexo.solver.Turn
import de.mineking.hexo.solver.transform
import kotlinx.coroutines.await
import org.khronos.webgl.Int32Array
import org.khronos.webgl.set

actual class StrixHexoSolver actual constructor(
    private val depthCap: Int,
    private val nodeBudget: Int,
    private val engine: StrixSolverEngine,
) : HexoSolver {
    private val ready = init()

    @OptIn(ExperimentalWasmJsInterop::class)
    // lazy so this is only called once the wasm backend is initialized
    private val limits by lazy {
        SolverLimits(
            depthCap = depthCap,
            nodeBudget = jsBigInt(nodeBudget),
            engine = when (engine) {
                StrixSolverEngine.IterativeDeepeningThreatTable -> SolverEngine.Idtt
                StrixSolverEngine.ProofNumberSearch -> SolverEngine.Pns
                StrixSolverEngine.DepthFirstProofNumberSearch -> SolverEngine.Dfpn
                StrixSolverEngine.ProofAndDisproofNumberSearch -> SolverEngine.Pdspn
            },
        )
    }

    // lazy so this is only called once the wasm backend is initialized
    private val solver by lazy { StrixSolver() }

    actual override suspend fun findWin(board: Board, player: CellOwner, remaining: Int): FindWinResult {
        if (board.isEmpty(includeHighlights = false)) return FindWinResult.NoWin
        ready.await()

        val position = board.toPosition(player, remaining)
        val outcome = solver.solveWide(position, limits)

        return outcome.toResult()
    }

    actual override suspend fun findDefense(board: Board, player: CellOwner, remaining: Int): FindDefenseResult {
        if (board.isEmpty(includeHighlights = false)) return FindDefenseResult.NoThreat
        ready.await()

        val transformed = board.transform()
        val position = transformed.board.toPosition(transformed.flipPlayer(player), remaining)
        val outcome = solver.solveDefenseWide(position, limits)

        return outcome.toResult(transformed)
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

    @Suppress("Indentation")
    private fun createPartialTurns(
        single: Array<CoordW>,
        full: Array<PairAnchor>,
        counterThreats: Array<PairAnchor>,
    ) = single
        .takeIf { it.isNotEmpty() }
        ?.map {
            PartialTurn(
                first = it.core,
                second = null,
                isCounterThreat = false,
            )
        } ?: full.map {
            PartialTurn(
                first = it.first.core,
                second = it.second.core,
                isCounterThreat = it in counterThreats,
            )
        }

    private fun DefenseOutcome.createDefense(transform: BoardTransformResult): DefenseResult {
        val defenses = createPartialTurns(killers, pairAnchors, counterThreats)
            .map { transform.transformBack(it) }

        if (defenses.isNotEmpty()) return DefenseResult.Found(defenses)

        val maybeDefenses = createPartialTurns(unresolved, tacticalPairs, emptyArray())
            .map { transform.transformBack(it) }

        if (maybeDefenses.isNotEmpty()) return DefenseResult.BudgetExceeded(maybeDefenses)

        return DefenseResult.Undefendable(
            bestDelay?.core?.let {
                PartialTurn(
                    first = transform.transformBack(it),
                    second = null,
                    isCounterThreat = false,
                )
            },
        )
    }

    private fun DefenseOutcome.toResult(transform: BoardTransformResult) = when (kind) {
        DefenseKind.NoThreat -> FindDefenseResult.NoThreat
        DefenseKind.BudgetExceeded -> FindDefenseResult.Unknown
        DefenseKind.ThreatFound -> FindDefenseResult.Threat(
            threat = transform.transformBack(threat!!.toResult() as FindWinResult.Win),
            defense = createDefense(transform),
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
