package de.mineking.hexo.solver

import de.mineking.hexo.board.Board
import de.mineking.hexo.core.CellOwner

interface HexoSolver {
    suspend fun findWin(board: Board, player: CellOwner, remaining: Int = 2): FindWinResult
    suspend fun findDefense(board: Board, player: CellOwner, remaining: Int = 2): FindDefenseResult
}
