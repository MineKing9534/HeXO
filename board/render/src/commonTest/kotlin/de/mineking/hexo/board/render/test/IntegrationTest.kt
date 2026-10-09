package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.parse.notation.CombinedNotationParser
import de.mineking.hexo.board.parse.notation.parseRectilinearNotation
import de.mineking.hexo.board.render.notation.BKENotationBoardRenderer
import de.mineking.hexo.board.render.notation.RectilinearNotationType
import de.mineking.hexo.board.render.notation.renderCombinedNotation
import de.mineking.hexo.board.render.notation.renderRectilinearNotation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IntegrationTest {
    private fun integrationTest(type: RectilinearNotationType) {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[3, 0].owner = CellOwner.X
        board[0, 1].owner = CellOwner.O
        board[2, 1].owner = CellOwner.O
        board[0, 3].owner = CellOwner.X

        val rendered = board.renderRectilinearNotation(type)
        val parsed = rendered.parseRectilinearNotation()

        assertEquals(board, parsed)
    }

    @Test
    fun `compact integration test`() {
        integrationTest(RectilinearNotationType.Compact)
    }

    @Test
    fun `multiline integration test`() {
        integrationTest(RectilinearNotationType.Multiline)
    }

    @Test
    fun `focused empty cells do not extend rectilinear notation`() {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[0, 3].focused = true

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)

        assertEquals("x", rendered)
    }

    @Test
    fun `compact with highlight`() {
        val board = MutableBoard()
        board[0, 0].apply {
            owner = CellOwner.X
            highlight = CellHighlight(null)
        }
        board[1, 0].highlight = CellHighlight(null)
        board[3, 0].owner = CellOwner.X
        board[0, 1].owner = CellOwner.O
        board[2, 1].owner = CellOwner.O
        board[0, 3].owner = CellOwner.X

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        val parsed = rendered.parseRectilinearNotation()

        assertEquals(board, parsed)
    }

    @Test
    fun `label integration test`() {
        val board = MutableBoard()
        board[0, 0].apply {
            owner = CellOwner.X
            label = "a"
        }
        board[1, 0].label = "b"

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        val parsed = rendered.parseRectilinearNotation()

        assertEquals(board, parsed)
    }

    @Test
    fun `board renderer produces BKE notation`() = runTest {
        val board = MutableBoard()
        board[0, 0].apply {
            owner = CellOwner.X
            turn = 0
        }
        board[1, 0].apply {
            owner = CellOwner.O
            turn = 1
        }
        assertEquals("o A0", BKENotationBoardRenderer.render(board, Unit))
    }

    @Test
    fun `combined notation includes every turn with its original orientation`() = runTest {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[1, 0].apply {
            owner = CellOwner.O
            turn = 1
        }
        board[0, 1].apply {
            owner = CellOwner.O
            turn = 1
        }
        board[0, -1].apply {
            owner = CellOwner.X
            turn = 2
        }

        val notation = board.renderCombinedNotation()
        val parsed = CombinedNotationParser.Default.parse(notation)
        assertEquals(board.cells, parsed.cells)
    }

    @Test
    fun `combined origin uses an initial cell away from zero even when zero is a move`() = runTest {
        val board = MutableBoard()
        board[5, 3].owner = CellOwner.X
        board[0, 0].apply {
            owner = CellOwner.O
            turn = 1
        }

        val notation = board.renderCombinedNotation()
        val parsed = CombinedNotationParser.Default.parse(notation)
        assertEquals("x[*]", notation.substringBefore(" || "))
        assertEquals(
            mapOf(
                CellCoordinate.Zero to Cell(CellOwner.X),
                CellCoordinate(-5, -3) to Cell(CellOwner.O, turn = 1),
            ),
            parsed.cells,
        )
        assertEquals("", board[5, 3].label)
    }

    @Test
    fun `combined origin minimizes ring sizes among initial cells`() = runTest {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[10, 0].owner = CellOwner.X
        board[11, 0].apply {
            owner = CellOwner.O
            turn = 1
        }

        val notation = board.renderCombinedNotation()
        val parsed = CombinedNotationParser.Default.parse(notation)
        assertEquals("x9x[*] || > CW o A0", notation)
        assertEquals(board.cells, parsed.cells)
    }

    @Test
    fun `combined origin preserves existing labels and avoids later moves`() = runTest {
        val board = MutableBoard()
        board[0, 0].apply {
            owner = CellOwner.X
            label = "keep"
        }
        board[1, 0].apply {
            owner = CellOwner.O
            turn = 1
        }

        val notation = board.renderCombinedNotation()
        val parsed = CombinedNotationParser.Default.parse(notation)
        assertEquals(board.cells + (CellCoordinate(0, 1) to Cell.EMPTY), parsed.cells)
        assertEquals("keep", board[0, 0].label)
    }

    @Test
    fun `combined origin fallback skips all occupied neighboring moves`() = runTest {
        val board = MutableBoard()
        board[0, 0].apply {
            owner = CellOwner.X
            label = "keep"
        }
        val moves = Direction.entries.map { it.direction }
        moves.forEach { coordinate ->
            board[coordinate].apply {
                owner = CellOwner.O
                turn = 1
            }
        }

        val notation = board.renderCombinedNotation()
        val initialState = notation.substringBefore(" || ").parseRectilinearNotation()
        val parsed = CombinedNotationParser.Default.parse(notation)
        assertEquals(CellCoordinate(2, 0), initialState.cells.entries.single { it.value.label == "*" }.key)
        assertEquals(board.cells + (CellCoordinate(2, 0) to Cell.EMPTY), parsed.cells)
        assertEquals(7, board.cells.size)
    }
}
