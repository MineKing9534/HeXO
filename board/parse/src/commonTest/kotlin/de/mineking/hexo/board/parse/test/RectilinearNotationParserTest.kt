package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.parse.parseRectilinearNotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RectilinearNotationParserTest {
    @Test
    fun `parse compact simple`() {
        val board = "x-x/o.o//x".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X),
                CellCoordinate(3, 0) to Cell(CellOwner.X),
                CellCoordinate(0, 1) to Cell(CellOwner.O),
                CellCoordinate(2, 1) to Cell(CellOwner.O),
                CellCoordinate(0, 3) to Cell(CellOwner.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse column notation`() {
        val board = "cx-x/o.o//x".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X),
                CellCoordinate(0, 3) to Cell(CellOwner.X),
                CellCoordinate(1, 0) to Cell(CellOwner.O),
                CellCoordinate(1, 2) to Cell(CellOwner.O),
                CellCoordinate(3, 0) to Cell(CellOwner.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse compact with single digit`() {
        val board = "x2x/o1o//x".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X),
                CellCoordinate(3, 0) to Cell(CellOwner.X),
                CellCoordinate(0, 1) to Cell(CellOwner.O),
                CellCoordinate(2, 1) to Cell(CellOwner.O),
                CellCoordinate(0, 3) to Cell(CellOwner.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse compact with long number`() {
        val board = "x10x".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X),
                CellCoordinate(11, 0) to Cell(CellOwner.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse multiline`() {
        val board = """
            x . .  x
             o . o
            
               x
        """.trimIndent().parseRectilinearNotation()

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X),
                CellCoordinate(3, 0) to Cell(CellOwner.X),
                CellCoordinate(0, 1) to Cell(CellOwner.O),
                CellCoordinate(2, 1) to Cell(CellOwner.O),
                CellCoordinate(0, 3) to Cell(CellOwner.X),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse with cell highlight`() {
        val board = "x x(o) - .(!) o(!)".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X),
                CellCoordinate(1, 0) to Cell(CellOwner.X, highlight = CellHighlight(CellOwner.O)),
                CellCoordinate(4, 0) to Cell(null, highlight = CellHighlight(null)),
                CellCoordinate(5, 0) to Cell(CellOwner.O, highlight = CellHighlight(null)),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse legacy cell highlight`() {
        val board = "x X - ! O".parseRectilinearNotation()
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X),
                CellCoordinate(1, 0) to Cell(CellOwner.X, highlight = CellHighlight(null)),
                CellCoordinate(4, 0) to Cell(null, highlight = CellHighlight(null)),
                CellCoordinate(5, 0) to Cell(CellOwner.O, highlight = CellHighlight(null)),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse line highlight`() {
        val board = "..(<3x)/x/o(b)".parseRectilinearNotation()

        assertEquals(
            mapOf(
                CellCoordinate(0, 1) to Cell(CellOwner.X),
                CellCoordinate(0, 2) to Cell(CellOwner.O),
            ),
            board.cells,
        )

        assertEquals(
            listOf(
                LineHighlight(CellCoordinate(1, 0), Direction.Left, length = 3, color = CellOwner.X),
                LineHighlight(CellCoordinate(0, 2), Direction.TopLeft, length = 6, color = null),
            ),
            board.lineHighlights,
        )
    }

    @Test
    fun `parse with label`() {
        val board = """
            x . [a] . o [b]
        """.trimIndent().parseRectilinearNotation()

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X),
                CellCoordinate(1, 0) to Cell(null, label = "a"),
                CellCoordinate(3, 0) to Cell(CellOwner.O, label = "b"),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse with label on empty row`() {
        val e = assertFailsWith<HexoNotationException> {
            val _ = " x/[a]".parseRectilinearNotation()
        }

        assertEquals("This operations requires a cell in the current row!", e.message)
    }

    @Test
    fun `parse with unterminated label at eof`() {
        val e = assertFailsWith<HexoNotationException> {
            val _ = "x.[ab".parseRectilinearNotation()
        }
        assertEquals("Unterminated symbol at end of input: `ab`", e.message)
    }
}
