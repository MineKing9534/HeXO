package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.parse.parseRectilinearNotation
import de.mineking.hexo.board.parse.parseRectilinearStateBKETurnNotation
import de.mineking.hexo.board.render.notation.RectilinearNotationType
import de.mineking.hexo.board.render.notation.renderRectilinearNotation
import de.mineking.hexo.board.render.notation.renderRectilinearStateBKETurnNotation
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
    fun `bke test 1`() {
        val input = "o A0"
        val parsed = input.parseRectilinearStateBKETurnNotation()
        val rendered = parsed.renderRectilinearStateBKETurnNotation()

        assertEquals(input, rendered)
    }

    @Test
    fun `bke test 2`() {
        val input = "o A0 A2 x A1 A4 o B1.0 B4.0"
        val parsed = input.parseRectilinearStateBKETurnNotation()
        val rendered = parsed.renderRectilinearStateBKETurnNotation()

        assertEquals(input, rendered)
    }

    @Test
    fun `rectilinear state bke turn test`() {
        val input = ".x/xx, d o A0 B1 x B2.0 B2.1"
        val parsed = input.parseRectilinearStateBKETurnNotation()
        val rendered = parsed.renderRectilinearStateBKETurnNotation()

        assertEquals(".x/xx, b @(1, 0) o A0 A1 x B3.1 B4.0", rendered)
        val _ = rendered.parseRectilinearStateBKETurnNotation()
    }

    @Test
    fun `rectilinear state bke turn preserves shared offset`() {
        val board = MutableBoard()
        board[4, 0].owner = CellOwner.X
        board[0, 0].apply {
            owner = CellOwner.O
            turn = 1
        }

        val rendered = board.renderRectilinearStateBKETurnNotation()

        assertEquals("x, > @(-5, 0) o A0", rendered)
    }

    @Test
    fun `rectilinear state bke turn optimizes fixed origin direction`() {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[1, 0].apply {
            owner = CellOwner.O
            turn = 0
        }
        board[1, 1].apply {
            owner = CellOwner.O
            turn = 1
        }

        val rendered = board.renderRectilinearStateBKETurnNotation()

        assertEquals("x, q @(1, 0) o A0", rendered)
    }

    @Test
    fun `bke origin is not placed on a bke move`() {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[2, 0].apply {
            owner = CellOwner.O
            turn = 1
        }
        board[1, 0].apply {
            owner = CellOwner.O
            turn = 1
        }

        val rendered = board.renderRectilinearStateBKETurnNotation()

        assertEquals("x, q @(2, -1) o A0 A1", rendered)
        assertEquals(board, rendered.parseRectilinearStateBKETurnNotation())
    }
}
