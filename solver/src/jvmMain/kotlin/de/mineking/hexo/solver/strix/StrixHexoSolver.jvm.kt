package de.mineking.hexo.solver.strix

import cc.tyto.DefenseKind
import cc.tyto.DefenseResponse
import cc.tyto.Player
import cc.tyto.SolveKind
import cc.tyto.SolveRequest
import cc.tyto.SolveResponse
import cc.tyto.SolverEngine
import cc.tyto.Stone
import cc.tyto.StrixSolverLib
import cc.tyto.ThreatContainer
import de.mineking.hexo.board.Board
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
import kotlinx.serialization.json.Json

actual class StrixHexoSolver actual constructor(
    private val depthCap: Int,
    private val nodeBudget: Int,
    private val engine: StrixSolverEngine,
) : HexoSolver {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    actual override suspend fun findWin(board: Board, player: CellOwner, remaining: Int): FindWinResult {
        if (board.isEmpty(includeHighlights = false)) return FindWinResult.NoWin

        val request = createRequest(board, player, remaining)
        val responsePtr = StrixSolverLib.INSTANCE.solve(json.encodeToString(request))

        try {
            val response = json.decodeFromString<SolveResponse>(responsePtr.getString(0))
            return response.toResult()
        } finally {
            StrixSolverLib.INSTANCE.freeString(responsePtr)
        }
    }

    actual override suspend fun findDefense(board: Board, player: CellOwner, remaining: Int): FindDefenseResult {
        if (board.isEmpty(includeHighlights = false)) return FindDefenseResult.NoThreat

        val transformed = board.transform()
        val request = createRequest(transformed.board, transformed.flipPlayer(player), remaining)
        val responsePtr = StrixSolverLib.INSTANCE.solveDefense(json.encodeToString(request))

        try {
            val response = json.decodeFromString<DefenseResponse>(responsePtr.getString(0))
            return response.toResult(transformed)
        } finally {
            StrixSolverLib.INSTANCE.freeString(responsePtr)
        }
    }

    private fun createRequest(board: Board, player: CellOwner, remaining: Int) = SolveRequest(
        winLength = 6,
        placementRadius = 8,
        maxMoves = 300,
        toMove = player.strix,
        movesRemaining = remaining,
        depthCap = depthCap,
        nodeBudget = nodeBudget,
        engine = when (engine) {
            StrixSolverEngine.IterativeDeepeningThreatTable -> SolverEngine.Idtt
            StrixSolverEngine.ProofNumberSearch -> SolverEngine.Pns
            StrixSolverEngine.DepthFirstProofNumberSearch -> SolverEngine.Dfpn
            StrixSolverEngine.ProofAndDisproofNumberSearch -> SolverEngine.Pdspn
        },
        wide = true,
        stones = board.toStones(),
    )

    private fun Board.toStones() = cells.mapNotNull { (coordinate, cell) ->
        val owner = cell.owner ?: return@mapNotNull null
        Stone(
            q = coordinate.q,
            r = coordinate.r,
            player = owner.strix,
        )
    }

    private fun createPartialTurns(
        single: List<CellCoordinate>,
        full: List<Pair<CellCoordinate, CellCoordinate>>,
        counterThreats: List<Pair<CellCoordinate, CellCoordinate>>,
    ) = single
        .takeIf { it.isNotEmpty() }
        ?.map {
            PartialTurn(
                first = it,
                second = null,
                isCounterThreat = false,
            )
        } ?: full.map {
        PartialTurn(
            first = it.first,
            second = it.second,
            isCounterThreat = it in counterThreats,
        )
    }

    private fun DefenseResponse.createDefense(transform: BoardTransformResult): DefenseResult {
        val defenses = createPartialTurns(killers, pairAnchors, counterThreats)
            .map { transform.transformBack(it) }

        if (defenses.isNotEmpty()) return DefenseResult.Found(defenses)

        val maybeDefenses = createPartialTurns(unresolved, tacticalPairs, emptyList())
            .map { transform.transformBack(it) }

        if (maybeDefenses.isNotEmpty()) return DefenseResult.BudgetExceeded(maybeDefenses)

        return DefenseResult.Undefendable(
            bestDelay?.let {
                PartialTurn(
                    first = transform.transformBack(it),
                    second = null,
                    isCounterThreat = false,
                )
            },
        )
    }

    @Suppress("TooGenericExceptionThrown")
    private fun DefenseResponse.toResult(transform: BoardTransformResult) = when (kind) {
        DefenseKind.NoThreat -> FindDefenseResult.NoThreat
        DefenseKind.BudgetExceeded -> FindDefenseResult.Unknown
        DefenseKind.Error -> throw RuntimeException(error)
        DefenseKind.ThreatFound -> FindDefenseResult.Threat(
            threat = transform.transformBack(threat!!.toWinResult()),
            defense = createDefense(transform),
        )
    }

    @Suppress("TooGenericExceptionThrown")
    private fun SolveResponse.toResult() = when (kind) {
        SolveKind.No -> FindWinResult.NoWin
        SolveKind.BudgetExceeded -> FindWinResult.Unknown
        SolveKind.Error -> throw RuntimeException(error)
        SolveKind.Win -> toWinResult()
    }

    private fun ThreatContainer.toWinResult() = FindWinResult.Win(
        turns = pv.map { turn ->
            Turn(
                player = turn.player.core,
                cells = turn.cells,
            )
        },
    )
}

private val Player.core get() = CellOwner.entries[value - 1]
private val CellOwner.strix get() = Player(ordinal + 1)
