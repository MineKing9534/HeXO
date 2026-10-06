package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.parse.notation.CombinedNotationParser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CombinedNotationParserTest {
    @Test
    fun `translate turn coordinates to the labeled cell`() = runTest {
        val result = CombinedNotationParser.Default.parse(".x[*]/xx --- d CW o A0 A1")

        assertEquals(
            mapOf(
                CellCoordinate(1, 0) to Cell(CellOwner.X),
                CellCoordinate(0, 1) to Cell(CellOwner.X),
                CellCoordinate(1, 1) to Cell(CellOwner.X),
                CellCoordinate(2, -1) to Cell(CellOwner.O, turn = 1),
                CellCoordinate(2, 0) to Cell(CellOwner.O, turn = 1),
            ),
            result.cells,
        )
    }

    @Test
    fun `reject overlapping stones regardless of player`() = runTest {
        for (player in listOf("x", "o")) {
            val e = assertFailsWith<HexoNotationException> {
                CombinedNotationParser.Default.parse("x[*]x --- > CW $player A0")
            }
            assertEquals("Turn move at (1, 0) overlaps the initial state", e.message)
        }
    }

    @Test
    fun `reject overlap after translating to a nonzero origin`() = runTest {
        val e = assertFailsWith<HexoNotationException> {
            CombinedNotationParser.Default.parse(".x[*]x --- > CW o A0")
        }
        assertEquals("Turn move at (2, 0) overlaps the initial state", e.message)
    }

    @Test
    fun `reject overlap in later turns`() = runTest {
        val e = assertFailsWith<HexoNotationException> {
            CombinedNotationParser.Default.parse("x[*]x --- > CW o A1 x A0")
        }
        assertEquals("Turn move at (1, 0) overlaps the initial state", e.message)
    }

    @Test
    fun `reject implicit opening moves on occupied origins`() = runTest {
        for (turns in listOf("o A0", "0")) {
            val e = assertFailsWith<HexoNotationException> {
                CombinedNotationParser.Default.parse("x[*] --- $turns")
            }
            assertEquals("Turn move at (0, 0) overlaps the initial state", e.message)
        }
    }

    @Test
    fun `allow moves on empty labeled or highlighted cells`() = runTest {
        for ((notation, expectedCell) in listOf(
            ".[target]" to Cell(CellOwner.O, turn = 1, label = "target"),
            ".(!)" to Cell(CellOwner.O, turn = 1, highlight = CellHighlight(null)),
        )) {
            val result = CombinedNotationParser.Default.parse("x[*]$notation --- > CW o A0")
            assertEquals(
                mapOf(CellCoordinate.Zero to Cell(CellOwner.X), CellCoordinate(1, 0) to expectedCell),
                result.cells,
            )
        }
    }
}
