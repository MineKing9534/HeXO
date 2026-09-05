package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.TurnMetaData
import de.mineking.hexo.board.findNextTurn
import de.mineking.hexo.board.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.board.toGamePosition
import kotlin.test.Test
import kotlin.test.assertEquals

class RectilinearStateBKETurnNotationParserTest {
    @Test
    fun `follow turns after an explicit X turn following the implicit opening`() {
        val board = "x A0 H2.2 o A1 G2.2".parseRectilinearStateBKETurnNotation(focusWinningRows = false)

        val position = board.toGamePosition().turns
        assertEquals(listOf(CellOwner.O, CellOwner.X, CellOwner.O), position.turns.map { it.meta.player })
        assertEquals(listOf(1, 2, 2), position.turns.map { it.moves.size })
        assertEquals(TurnMetaData(CellOwner.X, 2, 3), board.findNextTurn())
    }

    @Test
    fun `continue an incomplete explicit X turn following the implicit opening`() {
        val board = "x A0".parseRectilinearStateBKETurnNotation(focusWinningRows = false)

        assertEquals(TurnMetaData(CellOwner.X, 1, 1), board.findNextTurn())
    }

    @Test
    fun `test 1`() {
        val board = ".x/xx, b@(1,0): o A0 A1 x B3.1 B3.2".parseRectilinearStateBKETurnNotation()
        assertEquals(
            mapOf(
                CellCoordinate(1, 0) to Cell(CellOwner.X),
                CellCoordinate(0, 1) to Cell(CellOwner.X),
                CellCoordinate(1, 1) to Cell(CellOwner.X),

                CellCoordinate(1, -1) to Cell(CellOwner.O, turn = 1),
                CellCoordinate(2, -1) to Cell(CellOwner.O, turn = 1),

                CellCoordinate(-1, 2) to Cell(CellOwner.X, turn = 2),
                CellCoordinate(0, 2) to Cell(CellOwner.X, turn = 2),
            ),
            board.cells,
        )
    }

    @Test
    fun `test highlight lines`() {
        val board = "x(!)(>)x, > x A1 B1".parseRectilinearStateBKETurnNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X, highlight = CellHighlight(null)),
                CellCoordinate(1, 0) to Cell(CellOwner.X),
                CellCoordinate(0, 1) to Cell(CellOwner.X, turn = 1),
                CellCoordinate(1, 1) to Cell(CellOwner.X, turn = 1),
            ),
            board.cells,
        )
        assertEquals(
            listOf(
                LineHighlight(CellCoordinate.Zero, Direction.Right, 6, null),
            ),
            board.lineHighlights,
        )
    }
}
