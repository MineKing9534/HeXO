package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.render.notation.RectilinearNotationType
import de.mineking.hexo.board.render.notation.renderRectilinearNotation
import kotlin.test.Test
import kotlin.test.assertEquals

class RectilinearNotationRenderTest {
    @Test
    fun `render compact`() {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[3, 0].owner = CellOwner.X
        board[0, 1].owner = CellOwner.O
        board[2, 1].owner = CellOwner.O
        board[0, 3].owner = CellOwner.X

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        assertEquals(
            "x-x/o.o//x",
            rendered,
        )
    }

    @Test
    fun `render compact minimize prefix single line`() {
        val board = MutableBoard()
        board[10, 0].owner = CellOwner.X

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        assertEquals("x", rendered)
    }

    @Test
    fun `render compact minimize prefix multiple lines`() {
        val board = MutableBoard()
        board[10, 0].owner = CellOwner.X
        board[10, 1].owner = CellOwner.O

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        assertEquals("x/o", rendered)
    }

    @Test
    fun `render compact long empty space`() {
        val board = MutableBoard()
        board[10, 0].owner = CellOwner.X
        board[0, 1].owner = CellOwner.O

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Compact)
        assertEquals("10x/o", rendered)
    }

    @Test
    fun `render multiline`() {
        val board = MutableBoard()
        board[0, 0].owner = CellOwner.X
        board[3, 0].owner = CellOwner.X
        board[0, 1].owner = CellOwner.O
        board[2, 1].owner = CellOwner.O
        board[0, 3].owner = CellOwner.X

        val rendered = board.renderRectilinearNotation(RectilinearNotationType.Multiline)
        assertEquals(
            """
                x . . x
                 o . o
                
                   x
            """.trimIndent(),
            rendered,
        )
    }
}
