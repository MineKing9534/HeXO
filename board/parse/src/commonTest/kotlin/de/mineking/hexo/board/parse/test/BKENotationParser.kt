package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.parse.notation.parseBKENotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BKENotationParser {
    @Test
    fun `parse long sword`() {
        val board = "o A0 A2 x A1 A4 o B1.0 D4.0".parseBKENotation()
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
    fun `parse counterclockwise context without an implicit opening move`() {
        val board = "p CCW x A0 A1 o A3 A4 x B0 B1".parseBKENotation()

        assertEquals(
            mapOf(
                CellCoordinate(-1, 1) to Cell(CellOwner.X, turn = 1),
                CellCoordinate(0, 1) to Cell(CellOwner.X, turn = 1),

                CellCoordinate(1, -1) to Cell(CellOwner.O, turn = 2),
                CellCoordinate(0, -1) to Cell(CellOwner.O, turn = 2),

                CellCoordinate(-2, 2) to Cell(CellOwner.X, turn = 3),
                CellCoordinate(-1, 2) to Cell(CellOwner.X, turn = 3),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse one move per turn`() {
        val board = "x A0 o A1".parseBKENotation()

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.O, turn = 0),
                CellCoordinate(1, -1) to Cell(CellOwner.X, turn = 1),
                CellCoordinate(1, 0) to Cell(CellOwner.O, turn = 2),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse three moves per turn`() {
        val board = "x A0 A1 A2 o B0 B1 B2".parseBKENotation()

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.O, turn = 0),

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
    fun `reject consecutive turns for the same player`() {
        for (notation in listOf(
            "x A0 x A1",
            "o A0 o A1",
            "> CW x A0 x A1",
            "p CCW o A0 o A1",
            "x A0 o A1 o A2",
        )) {
            assertFailsWith<HexoNotationException>(notation) { notation.parseBKENotation() }
        }
    }

    @Test
    fun `parse multi-letter rings`() {
        val board = "x Z0 AA0 AB0".parseBKENotation()

        assertEquals(CellOwner.X, board.cells[CellCoordinate(26, -26)]?.owner)
        assertEquals(CellOwner.X, board.cells[CellCoordinate(27, -27)]?.owner)
        assertEquals(CellOwner.X, board.cells[CellCoordinate(28, -28)]?.owner)
    }

    @Test
    fun `parse origin only`() {
        val board = "0".parseBKENotation()
        assertEquals(mapOf(CellCoordinate.Zero to Cell(CellOwner.X, turn = 0)), board.cells)
    }

    @Test
    fun `parse clockwise context without an implicit opening move`() {
        val board = "> CW x A0 A1 o B11".parseBKENotation()
        assertEquals(
            mapOf(
                CellCoordinate(1, 0) to Cell(CellOwner.X, turn = 1),
                CellCoordinate(0, 1) to Cell(CellOwner.X, turn = 1),
                CellCoordinate(2, -1) to Cell(CellOwner.O, turn = 2),
            ),
            board.cells,
        )
    }

    @Test
    fun `sector addressing matches perimeter offsets in both chiralities`() {
        for (chirality in listOf("CW", "CCW")) {
            val sector = "d $chirality x C1.2 C5.2".parseBKENotation()
            val perimeter = "d $chirality x C5 C17".parseBKENotation()
            assertEquals(perimeter.cells, sector.cells)
        }
    }

    @Test
    fun `reject duplicate coordinates including sector aliases`() {
        for (notation in listOf("x A0 A0", "x A0 o A0", "> CCW x B3 o B1.1")) {
            assertFailsWith<HexoNotationException>(notation) { notation.parseBKENotation() }
        }
    }

    @Test
    fun `reject invalid offsets and sectors`() {
        for (move in listOf("A6", "B12", "B6.0", "B1.2", "A", "A.0", "A0.", "A0.0.0")) {
            assertFailsWith<HexoNotationException>(move) { "x $move".parseBKENotation() }
        }
    }

    @Test
    fun `reject overflowing ring labels and offsets`() {
        for (move in listOf("ZZZZZZZZ0", "A2147483648", "B2147483648.0")) {
            assertFailsWith<HexoNotationException>(move) { "x $move".parseBKENotation() }
        }
    }

    @Test
    fun `reject incomplete notation and trailing input`() {
        for (notation in listOf("", "x", "d x A0", "CW x A0", "0 x A0", "x A0 !")) {
            assertFailsWith<HexoNotationException>(notation) { notation.parseBKENotation() }
        }
    }
}
