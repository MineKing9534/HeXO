package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.parse.notation.BKENotationParser
import de.mineking.hexo.board.parse.notation.CombinedNotationParser
import de.mineking.hexo.board.parse.notation.RectilinearNotationParser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CombinedNotationParserTest {
    @Test
    fun `parse raw state without requiring or removing an origin label`() = runTest {
        for (notation in listOf("x.o", "x[*]", "x---x", "x[*]---x", "x[label||text]", "x[*] ---", "x[*] --- 0")) {
            assertEquals(RectilinearNotationParser.parse(notation), CombinedNotationParser.Default.parse(notation), notation)
        }
    }

    @Test
    fun `parse raw turns without a separator`() = runTest {
        for (notation in listOf("0", "x A0", "d CW o A0 A1")) {
            assertEquals(BKENotationParser.parse(notation), CombinedNotationParser.Default.parse(notation), notation)
        }
    }

    @Test
    fun `return raw state unchanged without trying the turn parser`() = runTest {
        val expected = Board(cells = mapOf(CellCoordinate.Zero to Cell(CellOwner.X, turn = 7, label = "*")))
        val stateParser = object : NotationParser {
            override suspend fun parse(notation: String): Board {
                assertEquals(" raw state ", notation)
                return expected
            }
        }
        val parser = CombinedNotationParser(stateParser, NotationParser.Companion.None)

        assertSame(expected, parser.parse(" raw state "))
    }

    @Test
    fun `try raw turns only after a state format mismatch`() = runTest {
        val expected = Board(cells = mapOf(CellCoordinate.Zero to Cell(CellOwner.O, turn = 1)))
        val stateParser = object : NotationParser {
            override suspend fun parse(notation: String): Board {
                assertEquals(" raw turns ", notation)
                throw HexoNotationFormatException("Not a state")
            }
        }
        val turnParser = object : NotationParser {
            override suspend fun parse(notation: String): Board {
                assertEquals(" raw turns ", notation)
                return expected
            }
        }

        assertSame(expected, CombinedNotationParser(stateParser, turnParser).parse(" raw turns "))
    }

    @Test
    fun `preserve state errors without trying the turn parser`() = runTest {
        val expected = HexoNotationException("Invalid state")
        val stateParser = object : NotationParser {
            override suspend fun parse(notation: String): Board = throw expected
        }
        val parser = CombinedNotationParser(stateParser, NotationParser.Companion.None)

        assertSame(expected, assertFailsWith<HexoNotationException> { parser.parse("state") })
    }

    @Test
    fun `preserve errors after labels containing a separator`() = runTest {
        for ((separator, notation) in listOf(
            "||" to "x[label||text] ?",
            "||" to "x[outer[inner||text]] ?",
            "||" to "x[label||text](?)",
            ":" to "x[label:text] ?",
        )) {
            val parser = CombinedNotationParser(RectilinearNotationParser, BKENotationParser, separator = separator)
            val expected = assertFailsWith<HexoNotationException>(notation) { RectilinearNotationParser.parse(notation) }
            val actual = assertFailsWith<HexoNotationException>(notation) { parser.parse(notation) }

            assertEquals(expected.message, actual.message, notation)
            assertEquals(expected::class, actual::class, notation)
        }
    }

    @Test
    fun `reject unrecognized combined inputs as format mismatches`() = runTest {
        for (notation in listOf("unrelated || x A0", "|| x A0", "x || x A0")) {
            assertFailsWith<HexoNotationFormatException>(notation) { CombinedNotationParser.Default.parse(notation) }
        }
    }

    @Test
    fun `support custom separator and origin label`() = runTest {
        val parser = CombinedNotationParser(RectilinearNotationParser, BKENotationParser, originLabel = "HOME", separator = "|")
        val result = parser.parse(".[HOME] | > CW o A0")

        assertEquals(
            mapOf(CellCoordinate.Zero to Cell.EMPTY, CellCoordinate(1, 0) to Cell(CellOwner.O, turn = 1)),
            result.cells,
        )
    }

    @Test
    fun `translate turn coordinates to the labeled cell`() = runTest {
        val result = CombinedNotationParser.Default.parse(".x[*]/xx || d CW o A0 A1")

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
                CombinedNotationParser.Default.parse("x[*]x || > CW $player A0")
            }
            assertEquals("Turn move at (1, 0) overlaps the initial state", e.message)
        }
    }

    @Test
    fun `reject overlap after translating to a nonzero origin`() = runTest {
        val e = assertFailsWith<HexoNotationException> {
            CombinedNotationParser.Default.parse(".x[*]x || > CW o A0")
        }
        assertEquals("Turn move at (2, 0) overlaps the initial state", e.message)
    }

    @Test
    fun `reject overlap in later turns`() = runTest {
        val e = assertFailsWith<HexoNotationException> {
            CombinedNotationParser.Default.parse("x[*]x || > CW o A1 x A0")
        }
        assertEquals("Turn move at (1, 0) overlaps the initial state", e.message)
    }

    @Test
    fun `reject implicit opening moves on occupied origins`() = runTest {
        for (turns in listOf("o A0", "x A0")) {
            val e = assertFailsWith<HexoNotationException> {
                CombinedNotationParser.Default.parse("x[*] || $turns")
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
            val result = CombinedNotationParser.Default.parse("x[*]$notation || > CW o A0")
            assertEquals(
                mapOf(CellCoordinate.Zero to Cell(CellOwner.X), CellCoordinate(1, 0) to expectedCell),
                result.cells,
            )
        }
    }
}
