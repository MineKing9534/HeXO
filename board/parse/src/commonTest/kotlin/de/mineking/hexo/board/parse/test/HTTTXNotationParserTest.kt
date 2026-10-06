package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.board.parse.notation.parseHTTTXNotation
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HTTTXNotationParserTest {
    @Test
    fun `parse sample notation`() {
        val board = """
            version[1];
            1. [0,1][1,-1];
            2. [1,0][-1,0];
            3. [2,0][-4,0];
        """.trimIndent().parseHTTTXNotation()

        assertEquals(
            mapOf(
                CellCoordinate(0, 0) to Cell(CellOwner.X, turn = 0),
                CellCoordinate(1, -1) to Cell(CellOwner.O, turn = 1),
                CellCoordinate(0, 1) to Cell(CellOwner.O, turn = 1),
                CellCoordinate(-1, 0) to Cell(CellOwner.X, turn = 2),
                CellCoordinate(1, 0) to Cell(CellOwner.X, turn = 2),
                CellCoordinate(-4, 0) to Cell(CellOwner.O, turn = 3),
                CellCoordinate(2, 0) to Cell(CellOwner.O, turn = 3),
            ),
            board.cells,
        )
    }

    @Test
    fun `apply visuals on the last move to occupied and empty cells`() {
        val board = $$"""
            version[1];
            1. [0,1][1,-1];
            2. [1,0][-1,0]<0,1:#:$A1><-2,1:$EMPTY2><0,0:#X><3,-2:#O><9,9>;
        """.trimIndent().parseHTTTXNotation()

        assertEquals(Cell(CellOwner.O, highlight = CellHighlight(null), turn = 1, label = "A1"), board.cells[CellCoordinate(1, -1)])
        assertEquals(Cell(label = "EMPTY2"), board.cells[CellCoordinate(-1, -1)])
        assertEquals(Cell(CellOwner.X, highlight = CellHighlight(null), turn = 0), board.cells[CellCoordinate.Zero])
        assertEquals(Cell(highlight = CellHighlight(null)), board.cells[CellCoordinate(1, 2)])
        assertEquals(7, board.cells.size)
    }

    @Test
    fun `combine visuals for the same coordinate in order`() {
        val board = $$"version[1]; 1. [1,0]<0,0:$FIRST><0,0:#Z><0,0:$SECOND>;".parseHTTTXNotation()

        assertEquals(Cell(CellOwner.X, highlight = CellHighlight(null), turn = 0, label = "SECOND"), board.cells[CellCoordinate.Zero])
    }

    @Test
    fun `reject malformed visuals`() {
        for (visual in listOf("<0,0:>", $$"<0,0:$>", $$"<0,0:$lower>", "<0,0:#AB>", $$"<0,0:$A:#>", "<0:#>", "<0,0:#")) {
            assertFailsWith<HexoNotationFormatException>(visual) {
                "version[1]; 1. [1,0]$visual;".parseHTTTXNotation()
            }
        }
    }

    @Test
    fun `reject visual coordinates outside the supported range`() {
        for (coordinate in listOf("2147483648,0", "2147483647,1", "0,-2147483648")) {
            assertFailsWith<HexoNotationException>(coordinate) {
                "version[1]; 1. [1,0]<$coordinate:#>;".parseHTTTXNotation()
            }
        }
    }

    @Test
    fun `reject duplicate coordinates including the opening move`() {
        for (notation in listOf("1. [0,0];", "1. [1,0][1,0];", "1. [1,0]; 2. [1,0];")) {
            assertFailsWith<HexoNotationException>(notation) {
                "version[1]; $notation".parseHTTTXNotation()
            }
        }
    }

    @Test
    fun `reject missing version`() {
        val e = assertFailsWith<HexoNotationFormatException> {
            "1. [1,0];".parseHTTTXNotation()
        }

        assertContains(e.message, "Invalid notation at 1:1:")
        assertContains(e.message, "version[")
    }

    @Test
    fun `reject turn number gaps`() {
        val e = assertFailsWith<HexoNotationException> {
            "version[1]; 2. [1,0];".parseHTTTXNotation()
        }

        assertEquals("Expected HTTTX turn `1` but found `2`", e.message)
    }
}
