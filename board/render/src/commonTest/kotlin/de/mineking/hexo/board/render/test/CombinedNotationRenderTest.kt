package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.parse.notation.CombinedNotationParser
import de.mineking.hexo.board.parse.notation.parseBKENotation
import de.mineking.hexo.board.parse.notation.parseRectilinearNotation
import de.mineking.hexo.board.render.notation.NotationType
import de.mineking.hexo.board.render.notation.RectilinearNotationType
import de.mineking.hexo.board.render.notation.renderCombinedNotation
import de.mineking.hexo.board.render.notation.renderRectilinearNotation
import de.mineking.hexo.board.translate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CombinedNotationRenderTest {
    @Test
    fun `combined notation type exports initial state and turns`() = runTest {
        val board = Board(cells = mapOf(
            CellCoordinate.Zero to Cell(CellOwner.X),
            CellCoordinate(1, 0) to Cell(CellOwner.O, turn = 1),
            CellCoordinate(0, 1) to Cell(CellOwner.O, turn = 1),
        ))
        val notation = NotationType.Combined.renderer.render(board, Unit)

        assertEquals("x[*] || > CW o A0 A1", notation)
        assertEquals(board.cells, CombinedNotationParser.Default.parse(notation).cells)
    }

    @Test
    fun `state only renders without a separator or synthetic origin label`() = runTest {
        val board = "x[keep].o".parseRectilinearNotation()
        val notation = board.renderCombinedNotation()

        assertEquals(board.renderRectilinearNotation(RectilinearNotationType.Compact), notation)
        assertEquals(board.cells, CombinedNotationParser.Default.parse(notation).cells)
    }

    @Test
    fun `turns only render ordinary BKE notation`() = runTest {
        val board = "o A0 A1".parseBKENotation()
        val notation = board.renderCombinedNotation()

        assertEquals("o A0 A1", notation)
        assertEquals(board.cells, CombinedNotationParser.Default.parse(notation).cells)
    }

    @Test
    fun `opening move only renders zero`() = runTest {
        val board = "0".parseBKENotation()

        assertEquals("0", board.renderCombinedNotation())
        assertEquals(board.cells, CombinedNotationParser.Default.parse(board.renderCombinedNotation()).cells)
    }

    @Test
    fun `single move partial turn renders as an implicit opening`() = runTest {
        val board = "> CW o A0".parseBKENotation()
        val notation = board.renderCombinedNotation()

        assertEquals("0", notation)
        assertEquals(
            mapOf(CellCoordinate.Zero to Cell(CellOwner.X, turn = 0)),
            CombinedNotationParser.Default.parse(notation).cells,
        )
    }

    @Test
    fun `single move first turn supplies the implicit origin for later turns`() = runTest {
        val board = Board(cells = mapOf(
            CellCoordinate.Zero to Cell(CellOwner.O, turn = 0),
            CellCoordinate(-1, 0) to Cell(CellOwner.X, turn = 1),
        ))
        val notation = board.renderCombinedNotation()

        assertEquals("x A0", notation)
        assertEquals(
            mapOf(
                CellCoordinate.Zero to Cell(CellOwner.O, turn = 0),
                CellCoordinate(1, -1) to Cell(CellOwner.X, turn = 1),
            ),
            CombinedNotationParser.Default.parse(notation).cells,
        )
    }

    @Test
    fun `multiple move first turn renders with an explicit origin`() = runTest {
        val board = "> CW o A0 A1".parseBKENotation()
        val notation = board.renderCombinedNotation()

        assertEquals("< CCW o A0 B1", notation)
        assertEquals(
            board.translate(CellCoordinate(-2, 0)).cells,
            CombinedNotationParser.Default.parse(notation).cells,
        )
    }

    @Test
    fun `multiple move first turn uses an unoccupied origin when zero is occupied`() = runTest {
        val board = Board(cells = mapOf(
            CellCoordinate.Zero to Cell(CellOwner.O, turn = 1),
            CellCoordinate(1, 0) to Cell(CellOwner.O, turn = 1),
            CellCoordinate(-1, 0) to Cell(CellOwner.X, turn = 2),
        ))
        val parsed = CombinedNotationParser.Default.parse(board.renderCombinedNotation())

        assertEquals(board.translate(CellCoordinate(0, -1)).cells, parsed.cells)
    }

    @Test
    fun `empty state and turns return the empty state notation`() {
        val board = Board()

        assertEquals(board.renderRectilinearNotation(RectilinearNotationType.Compact), board.renderCombinedNotation())
        assertEquals("", board.renderCombinedNotation())
    }

    @Test
    fun `invisible state cells do not add a separator to turn notation`() {
        val board = "0".parseBKENotation().copy()
        board[5, 5].focused = true

        assertEquals("0", board.renderCombinedNotation())
    }

    @Test
    fun `highlight only state is retained alongside turns`() = runTest {
        val board = MutableBoard()
        board[0, 0].highlight = CellHighlight(null)
        board[1, 0].apply {
            owner = CellOwner.O
            turn = 1
        }
        val notation = board.renderCombinedNotation()

        assertContains(notation, " || ")
        assertEquals(board.cells, CombinedNotationParser.Default.parse(notation).cells)
    }

    @Test
    fun `line highlight only state renders without turns`() = runTest {
        val board = MutableBoard()
        board.highlightLine(CellCoordinate.Zero, Direction.Right, 3)
        val notation = board.renderCombinedNotation()

        assertEquals(board.renderRectilinearNotation(RectilinearNotationType.Compact), notation)
        assertEquals(board.lineHighlights, CombinedNotationParser.Default.parse(notation).lineHighlights)
    }
}
