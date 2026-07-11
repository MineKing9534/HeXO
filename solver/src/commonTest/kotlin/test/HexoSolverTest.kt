package test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.findWinningRows
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.solver.FindDefenseResult
import de.mineking.hexo.solver.FindWinResult
import de.mineking.hexo.solver.HexoSolver
import de.mineking.hexo.solver.isDefendable
import de.mineking.hexo.solver.isLost
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

abstract class HexoSolverTest(val solver: HexoSolver) {
    private fun checkWin(board: Board, player: CellOwner, result: FindWinResult.Win) {
        val board = board.copy()
        result.turns.forEachIndexed { index, (owner, cells) ->
            cells.forEach { coordinate ->
                board[coordinate].owner = owner
                board[coordinate].turn = index + 1
            }
        }

        val rows = board.findWinningRows()
        assertFalse(rows.isEmpty())

        rows.forEach { (entry) ->
            assertEquals(entry.second.owner, player)
        }
    }

    @Test
    fun `solve no win`() = runTest {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[1, 0].owner = CellOwner.O
        board[-1, 0].owner = CellOwner.O

        val result = solver.findWin(board, CellOwner.X, 2)
        assertIs<FindWinResult.NoWin>(result)
    }

    @Test
    fun `solve open 3`() = runTest {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[1, 0].owner = CellOwner.X
        board[2, 0].owner = CellOwner.X

        val result = solver.findWin(board, CellOwner.X, 2)

        assertIs<FindWinResult.Win>(result)
        checkWin(board, CellOwner.X, result)
    }

    @Test
    fun `defend bone`() = runTest {
        val board = MutableBoard()
        board[-1, 0].owner = CellOwner.O
        board[3, 0].owner = CellOwner.O

        board[0, 0].owner = CellOwner.X
        board[1, 0].owner = CellOwner.X
        board[2, 0].owner = CellOwner.X

        board[1, 1].owner = CellOwner.X
        board[1, -1].owner = CellOwner.X

        val result = solver.findDefense(board, CellOwner.O, 2)

        assertIs<FindDefenseResult.Threat>(result)

        assertTrue(result.isLost())
        assertFalse(result.isDefendable())

        checkWin(board, CellOwner.X, result.threat)
    }
}
