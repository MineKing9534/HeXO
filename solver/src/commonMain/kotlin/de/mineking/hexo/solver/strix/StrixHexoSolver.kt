package de.mineking.hexo.solver.strix

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.solver.FindDefenseResult
import de.mineking.hexo.solver.FindWinResult
import de.mineking.hexo.solver.HexoSolver

expect class StrixHexoSolver(
    depthCap: Int = 10,
    nodeBudget: Int = 20_000,
    engine: StrixSolverEngine = StrixSolverEngine.IterativeDeepeningThreatTable,
) : HexoSolver {
    override suspend fun findWin(board: Board, player: CellOwner, remaining: Int): FindWinResult
    override suspend fun findDefense(board: Board, player: CellOwner, remaining: Int): FindDefenseResult
}
