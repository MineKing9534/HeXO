package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.parse.parseTytoNotation
import kotlin.test.Test
import kotlin.test.assertEquals

class TytoNotationParser {
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
