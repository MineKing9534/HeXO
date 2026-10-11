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
    fun `later turns break zero line and chirality ties`() = runTest {
        val board = createBoard(
            listOf(CellCoordinate.Zero),
            listOf(CellCoordinate(1, 0), CellCoordinate(-1, 0)),
            listOf(CellCoordinate(0, -1)),
        )
        assertEquals("o A0 A3 x A1", board.renderBKE())
    }

    @Test
    fun `lowest offset selects the zero line regardless of ring`() = runTest {
        val board = createBoard(
            listOf(CellCoordinate.Zero),
            listOf(CellCoordinate(1, 1), CellCoordinate(0, 1)),
        )
        assertEquals("o A0 B1", board.renderBKE())
    }

    @Test
    fun `ring A omits sector addressing for every offset`() = runTest {
        val expected = "o A0 A1 A2 A3 A4 A5"
        val board = expected.parseBKENotation()
        val notation = board.renderBKE()

        assertEquals(expected, notation)
        assertEquals(board.cells, notation.parseBKENotation().cells)
    }

    @Test
    fun `offsets are compared numerically`() = runTest {
        val board = createBoard(
            listOf(CellCoordinate.Zero),
            listOf(CellCoordinate(1, 0)),
            listOf(CellCoordinate(1, 2), CellCoordinate(-2, -1)),
        )
        assertEquals("o A0 x C2 C3.1", board.renderBKE())
    }

    @Test
    fun `earliest turn offsets take priority over later turns`() = runTest {
        val board = "o A0 A2 x A1 B2 o C2 C3 x C12 C15".parseBKENotation()

        assertEquals("o A0 A2", "o A0 A2".parseBKENotation().renderBKE())
        assertEquals("o A0 A2 x A1 B1.0", "o A0 A2 x A1 B2".parseBKENotation().renderBKE())
        assertEquals("o A0 A2 x A1 B1.0 o C2 C1.0 x C4.0 C5.0", board.renderBKE())
    }

    @Test
    fun `canonical example is independent of orientation and move order`() = runTest {
        val expected = "o A0 A2 x A1 B1.0 o C2 C1.0 x C4.0 C5.0"
        val reordered = "o A2 A0 x B2 A1 o C3 C2 x C15 C12"

        for (notation in listOf(expected, reordered)) {
            val original = notation.parseBKENotation()
            for (reflected in listOf(false, true)) {
                val board = if (reflected) original.mirror(Direction.Right) else original
                for (rotation in 0 until 6) {
                    val transformed = board.rotate(rotation).translate(CellCoordinate(7, -4))
                    assertEquals(expected, transformed.renderBKE())
                }
            }
        }
    }

    @Test
    fun `equal offset sequences use ring labels as a final tie breaker`() = runTest {
        val original = "o A0 B2 C1".parseBKENotation()
        for (reflected in listOf(false, true)) {
            val board = if (reflected) original.mirror(Direction.Right) else original
            for (rotation in 0 until 6) {
                assertEquals("o A0 C1 B1.0", board.rotate(rotation).renderBKE())
            }
        }
    }

    @Test
    fun `explicit origin prioritizes the first turn regardless of move order`() = runTest {
        for (notation in listOf("> CW x A0 A2 o A3 A4", "> CW x A2 A0 o A4 A3")) {
            val board = notation.parseBKENotation()
            val rendered = board.renderBKE(CellCoordinate.Zero)

            assertEquals("> CW x A0 A2 o A3 A4", rendered)
            assertEquals(board.cells, rendered.parseBKENotation().cells)
        }
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
        assertEquals("b CW x A0 o A2 A3", notation)
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
