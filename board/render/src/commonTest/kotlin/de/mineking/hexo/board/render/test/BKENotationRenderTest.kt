package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.MutableCell
import de.mineking.hexo.board.distanceTo
import de.mineking.hexo.board.mirror
import de.mineking.hexo.board.parse.notation.parseBKENotation
import de.mineking.hexo.board.render.notation.BKENotationBoardRenderer
import de.mineking.hexo.board.render.notation.renderBKENotation
import de.mineking.hexo.board.rotate
import de.mineking.hexo.board.toGamePositionForce
import de.mineking.hexo.board.translate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BKENotationRenderTest {
    private fun createBoard(vararg turns: List<CellCoordinate>, firstTurn: Int = 0): Board = MutableBoard().apply {
        turns.forEachIndexed { index, coordinates ->
            val owner = if (index % 2 == 0) CellOwner.X else CellOwner.O
            coordinates.forEach { coordinate ->
                this[coordinate] = MutableCell(owner, turn = firstTurn + index)
            }
        }
    }

    private suspend fun Board.renderBKE(origin: CellCoordinate? = null): String = if (origin == null) {
        BKENotationBoardRenderer.render(this, Unit)
    } else {
        // Explicit origins are only supported by the turn-based rendering API.
        toGamePositionForce().renderBKENotation(origin)
    }

    @Test
    fun `single opening move renders zero`() = runTest {
        assertEquals("0", createBoard(listOf(CellCoordinate(4, -3))).renderBKE())
    }

    @Test
    fun `implicit origin requires exactly one opening move`() = runTest {
        assertFailsWith<IllegalArgumentException> { createBoard().renderBKE() }
        assertFailsWith<IllegalArgumentException> {
            createBoard(listOf(CellCoordinate.Zero, CellCoordinate(1, 0))).renderBKE()
        }
    }

    @Test
    fun `opening move supplies a translated origin`() = runTest {
        val board = createBoard(
            listOf(CellCoordinate.Zero),
            listOf(CellCoordinate(1, 0), CellCoordinate(0, 1)),
        ).translate(CellCoordinate(4, -3))
        assertEquals("o A0 A1", board.renderBKE())
    }

    @Test
    fun `later turns break chirality ties`() = runTest {
        val board = createBoard(
            listOf(CellCoordinate.Zero),
            listOf(CellCoordinate(1, 0), CellCoordinate(-1, 0)),
            listOf(CellCoordinate(0, -1)),
        )
        assertEquals("o A0 A3 x A2", board.renderBKE())
    }

    @Test
    fun `next move breaks a zero line and chirality tie between sectors`() = runTest {
        val board = createBoard(
            listOf(CellCoordinate.Zero),
            listOf(CellCoordinate(1, 1), CellCoordinate(0, 1)),
        )
        assertEquals("o B1 A0", board.renderBKE())
    }

    @Test
    fun `offsets are compared numerically`() = runTest {
        val board = createBoard(
            listOf(CellCoordinate.Zero),
            listOf(CellCoordinate(1, 0)),
            listOf(CellCoordinate(1, 2), CellCoordinate(-2, -1)),
        )
        assertEquals("o A0 x C2 C10", board.renderBKE())
    }

    @Test
    fun `multi letter rings and variable turn sizes`() = runTest {
        val board = createBoard(
            listOf(CellCoordinate.Zero),
            listOf(CellCoordinate(26, 0), CellCoordinate(27, 0), CellCoordinate(28, 0)),
            listOf(CellCoordinate(53, 0)),
        )
        assertEquals("o Z0 AA0 AB0 x BA0", board.renderBKE())
    }

    @Test
    fun `explicit origin includes opening turn and preserves orientation`() = runTest {
        val board = createBoard(
            listOf(CellCoordinate(0, -1)),
            listOf(CellCoordinate(1, 0), CellCoordinate(0, 1)),
            firstTurn = 1,
        )
        val notation = board.renderBKE(origin = CellCoordinate.Zero)
        assertEquals("> CW x A4 o A0 A1", notation)
        assertEquals(board.cells, notation.parseBKENotation().cells)
    }

    @Test
    fun `explicit origin allows multiple opening moves and counterclockwise context`() = runTest {
        val origin = CellCoordinate(9, -4)
        val coordinates = listOf(CellCoordinate(1, 0), CellCoordinate(0, -1))
        val original = createBoard(coordinates, firstTurn = 1)
        val board = original.translate(origin)
        val notation = board.renderBKE(origin)
        assertEquals("> CCW x A0 A2", notation)
        assertEquals(original.cells, notation.parseBKENotation().cells)
    }

    @Test
    fun `explicit origin cannot be occupied by a move`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            createBoard(listOf(CellCoordinate.Zero)).renderBKE(CellCoordinate.Zero)
        }
    }

    @Test
    fun `canonical notation is invariant under translation rotation and reflection`() = runTest {
        val original = createBoard(
            listOf(CellCoordinate.Zero),
            listOf(CellCoordinate(2, 1), CellCoordinate(-1, 2)),
            listOf(CellCoordinate(1, -3), CellCoordinate(-2, 0)),
        )
        val expected = original.renderBKE()
        for (reflected in listOf(false, true)) {
            val board = if (reflected) original.mirror(Direction.Right) else original
            for (rotation in 0 until 6) {
                val transformed = board.rotate(rotation).translate(CellCoordinate(7, -4))
                assertEquals(expected, transformed.renderBKE())
            }
        }
    }

    @Test
    fun `explicit context round trips all ring sectors in both chiralities`() = runTest {
        val coordinates = (-5..5).flatMap { q ->
            (-5..5).map { r -> CellCoordinate(q, r) }
        }.filter { it.distanceTo(CellCoordinate.Zero) in 1..5 }

        for (first in listOf(CellCoordinate(1, 0), CellCoordinate(0, 1))) {
            for (second in listOf(CellCoordinate(0, -1), CellCoordinate(-1, 1))) {
                val ordered = listOf(first, second) + coordinates.filter { it != first && it != second }
                val board = createBoard(ordered, firstTurn = 1)
                val notation = board.renderBKE(CellCoordinate.Zero)
                assertEquals(board.cells, notation.parseBKENotation().cells)
            }
        }
    }
}
