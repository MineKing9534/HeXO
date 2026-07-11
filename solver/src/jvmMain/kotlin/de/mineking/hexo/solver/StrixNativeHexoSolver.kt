package de.mineking.hexo.solver

import cc.tyto.DefenseKind
import cc.tyto.DefenseResponse
import cc.tyto.Player
import cc.tyto.SolveKind
import cc.tyto.SolveRequest
import cc.tyto.SolveResponse
import cc.tyto.Stone
import cc.tyto.StrixSolverLib
import cc.tyto.ThreatContainer
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.core.CellOwner
import kotlinx.serialization.json.Json

class StrixNativeHexoSolver : HexoSolver {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun findWin(board: Board, player: CellOwner, remaining: Int): FindWinResult {
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

    override suspend fun findDefense(board: Board, player: CellOwner, remaining: Int): FindDefenseResult {
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
        depthCap = 10,
        nodeBudget = 20_000,
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

    private fun DefenseResponse.findDefenses() = killers
        .takeIf { it.isNotEmpty() }
        ?.map { Defense(it, null) }
        ?: pairAnchors.map { (first, second) -> Defense(first, second) }

    @Suppress("TooGenericExceptionThrown")
    private fun DefenseResponse.toResult(transform: BoardTransformResult) = when (kind) {
        DefenseKind.NoThreat -> FindDefenseResult.NoThreat
        DefenseKind.Error -> throw RuntimeException(error)
        DefenseKind.ThreatFound -> FindDefenseResult.Threat(
            threat = transform.transformBack(threat!!.toWinResult()),
            defenses = findDefenses().map { transform.transformBack(it) },
            bestDelay = bestDelay?.let { transform.transformBack(it) },
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
