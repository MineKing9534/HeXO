package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.parse.parseHTTTXNotation
import kotlin.test.Test
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
    fun `reject missing version`() {
        val e = assertFailsWith<HexoNotationException> {
            val _ = "1. [1,0];".parseHTTTXNotation()
        }

        assertEquals("HTTTX notation has to start with `version[n];`", e.message)
    }

    @Test
    fun `reject turn number gaps`() {
        val e = assertFailsWith<HexoNotationException> {
            val _ = "version[1]; 2. [1,0];".parseHTTTXNotation()
        }

        assertEquals("Expected HTTTX turn `1` but found `2`", e.message)
    }
}
