package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.parse.parseRectilinearStateBKETurnNotation
import kotlin.test.Test
import kotlin.test.assertEquals

class RectilinearStateBKETurnNotationParserTest {
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
