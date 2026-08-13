package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.parse.Chirality
import de.mineking.hexo.board.parse.parseBKENotation
import kotlin.test.Test
import kotlin.test.assertEquals

class BKENotationParser {
    @Test
    fun `parse long sword`() {
        val board = "o A0 A2 x A1 A4 o B1.0 D4.0".parseBKENotation(zeroOffsetLine = Direction.TopRight, origin = null)
        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X, turn = 0),

                CellCoordinate(0, 1) to Cell(CellOwner.O, turn = 1),
                CellCoordinate(1, -1) to Cell(CellOwner.O, turn = 1),

                CellCoordinate(1, 0) to Cell(CellOwner.X, turn = 2),
                CellCoordinate(-1, 0) to Cell(CellOwner.X, turn = 2),

                CellCoordinate(2, 0) to Cell(CellOwner.O, turn = 3),
                CellCoordinate(-4, 0) to Cell(CellOwner.O, turn = 3),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse x A0 A1 o A3 A4 x B0 B1`() {
        val board = "x A0 A1 o A3 A4 x B0 B1".parseBKENotation(
            zeroOffsetLine = Direction.BottomLeft,
            origin = null,
            chirality = Chirality.CounterClockwise,
        )

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X, turn = 0),

                CellCoordinate(-1, 1) to Cell(CellOwner.X, turn = 1),
                CellCoordinate(0, 1) to Cell(CellOwner.X, turn = 1),

                CellCoordinate(0, -1) to Cell(CellOwner.O, turn = 2),
                CellCoordinate(1, -1) to Cell(CellOwner.O, turn = 2),

                CellCoordinate(-2, 2) to Cell(CellOwner.X, turn = 3),
                CellCoordinate(-1, 2) to Cell(CellOwner.X, turn = 3),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse one move per turn`() {
        val board = "x A0 o A1".parseBKENotation(zeroOffsetLine = Direction.TopRight, origin = null)

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X, turn = 0),
                CellCoordinate(1, -1) to Cell(CellOwner.X, turn = 1),
                CellCoordinate(1, 0) to Cell(CellOwner.O, turn = 2),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse three moves per turn`() {
        val board = "x A0 A1 A2 o B0 B1 B2".parseBKENotation(zeroOffsetLine = Direction.TopRight, origin = null)

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X, turn = 0),

                CellCoordinate(1, -1) to Cell(CellOwner.X, turn = 1),
                CellCoordinate(1, 0) to Cell(CellOwner.X, turn = 1),
                CellCoordinate(0, 1) to Cell(CellOwner.X, turn = 1),

                CellCoordinate(2, -2) to Cell(CellOwner.O, turn = 2),
                CellCoordinate(2, -1) to Cell(CellOwner.O, turn = 2),
                CellCoordinate(2, 0) to Cell(CellOwner.O, turn = 2),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse multi-letter rings`() {
        val board = "x Z0 AA0 AB0".parseBKENotation(zeroOffsetLine = Direction.TopRight, origin = null)

        assertEquals(CellOwner.X, board.cells[CellCoordinate(26, -26)]?.owner)
        assertEquals(CellOwner.X, board.cells[CellCoordinate(27, -27)]?.owner)
        assertEquals(CellOwner.X, board.cells[CellCoordinate(28, -28)]?.owner)
    }
}
