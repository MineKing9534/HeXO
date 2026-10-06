package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.parse.notation.parseTytoNotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TytoNotationParser {
    @Test
    fun `parse origin only`() {
        val board = "".parseTytoNotation()
        assertEquals(mapOf(CellCoordinate.Zero to Cell(CellOwner.X, turn = 0)), board.cells)
    }

    @Test
    fun `parse incomplete final turn`() {
        val board = "AgAEAAYA".parseTytoNotation()
        assertEquals(
            mapOf(
                CellCoordinate.Zero to Cell(CellOwner.X, turn = 0),
                CellCoordinate(1, 0) to Cell(CellOwner.O, turn = 1),
                CellCoordinate(2, 0) to Cell(CellOwner.O, turn = 1),
                CellCoordinate(3, 0) to Cell(CellOwner.X, turn = 2),
            ),
            board.cells,
        )
    }

    @Test
    fun `reject duplicate coordinates including the opening move`() {
        for (notation in listOf("AAA", "AgACAA")) {
            assertFailsWith<HexoNotationException>(notation) { notation.parseTytoNotation() }
        }
    }

    @Test
    fun `reject truncated coordinates`() {
        assertFailsWith<HexoNotationException> { "Ag".parseTytoNotation() }
    }

    @Test
    fun `test longsword`() {
        val board = "AgEAAgIAAQAEAAcA".parseTytoNotation()
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
}
