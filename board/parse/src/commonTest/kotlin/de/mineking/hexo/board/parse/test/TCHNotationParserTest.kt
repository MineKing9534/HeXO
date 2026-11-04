package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.board.merge
import de.mineking.hexo.board.mirror
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.parse.notation.TCHNotationParser
import de.mineking.hexo.board.parse.notation.parseRectilinearNotation
import de.mineking.hexo.board.parse.notation.parseTCHNotation
import de.mineking.hexo.board.rotate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIsNot
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class TCHNotationParserTest {
    @Test
    fun `parse embedded notation with the default parser`() = runTest {
        for (notation in listOf(
            "x.o",
            "d CW o A0",
            "version[1]; 1. [1,0];",
            ".[*] || x A0",
        )) {
            assertEquals(
                NotationParser.Default.parse(notation),
                TCHNotationParser.parse($$"${$$notation}"),
                notation,
            )
        }
    }

    @Test
    fun `delegate embedded notation without delimiters or whitespace changes`() = runTest {
        val embeddedNotation = " \tx[label{nested}]\n  o \r\n"
        val expected = Board(cells = mapOf(CellCoordinate.Zero to Cell(CellOwner.X)))
        val parser = object : NotationParser {
            override suspend fun parse(notation: String): Board {
                assertEquals(embeddedNotation, notation)
                return expected
            }
        }

        assertSame(expected, $$"${$$embeddedNotation}".parseTCHNotation(parser))
    }

    @Test
    fun `rotate by positive negative zero and wrapped sector counts`() = runTest {
        val board = "x.o/.x".parseRectilinearNotation()

        for (sectors in listOf(Int.MIN_VALUE, -7, -1, 0, 1, 6, 7, Int.MAX_VALUE)) {
            assertEquals(
                board.rotate(sectors),
                TCHNotationParser.parse($$"$rotate($$sectors, {x.o/.x})"),
                "sectors=$sectors",
            )
        }
    }

    @Test
    fun `reject out of range integer arguments as notation errors`() = runTest {
        for (sectors in listOf("2147483648", "-2147483649", "999999999999999999999999999999")) {
            val notation = $$"$rotate($$sectors, {x})"
            val error = assertFailsWith<HexoNotationException>(notation) {
                notation.parseTCHNotation(NotationParser.Companion.None)
            }

            assertIsNot<HexoNotationFormatException>(error)
            assertEquals("Integer `$sectors` at 1 for 'rotate' is out of range", error.message)
        }
    }

    @Test
    fun `mirror around each supported axis`() = runTest {
        val board = "x.o/.x".parseRectilinearNotation()

        for ((axis, direction) in listOf("-" to Direction.Right, "/" to Direction.TopRight, "\\" to Direction.TopLeft)) {
            assertEquals(board.mirror(direction), TCHNotationParser.parse($$"$mirror($$axis, {x.o/.x})"), axis)
        }
    }

    @Test
    fun `evaluate nested transformations from the inside out`() = runTest {
        val board = "x.o/.x".parseRectilinearNotation()
        val expected = board.mirror(Direction.TopRight).rotate(1)
        assertNotEquals(expected, board.rotate(1).mirror(Direction.TopRight))

        assertEquals(expected, TCHNotationParser.parse($$"$rotate(1, mirror(/, {x.o/.x}))"))
    }

    @Test
    fun `compose multiple levels of transformations`() = runTest {
        val board = "x.o/.x".parseRectilinearNotation()
        val expected = board.rotate(-2).mirror(Direction.TopLeft).rotate(3)

        assertEquals(expected, TCHNotationParser.parse($$"$rotate(3, mirror(\\, rotate(-2, {x.o/.x})))"))
    }

    @Test
    fun `allow whitespace between expression tokens`() = runTest {
        val notation = " \n$ rotate \t( -1 , mirror ( - , {x.o/.x} ) ) \r\n"
        val expected = "x.o/.x".parseRectilinearNotation().mirror(Direction.Right).rotate(-1)

        assertEquals(expected, TCHNotationParser.parse(notation))
    }

    @Test
    fun `preserve labels highlights and turn metadata during transformations`() = runTest {
        for (notation in listOf("x[label](o)(>3x)/.o", "version[1]; 1. [1,0];")) {
            val expected = NotationParser.Default.parse(notation).rotate(1).mirror(Direction.TopRight)

            assertEquals(expected, TCHNotationParser.parse($$"$mirror(/, rotate(1, {$$notation}))"), notation)
        }
    }

    @Test
    fun `merge transformed boards and transform merged results`() = runTest {
        val expected = merge(listOf(
            "x".parseRectilinearNotation(),
            ".o".parseRectilinearNotation().rotate(1),
            "..o".parseRectilinearNotation().mirror(Direction.Right),
        )).rotate(-1)

        assertEquals(
            expected,
            TCHNotationParser.parse($$"$rotate(-1, merge({x}, rotate(1, {.o}), mirror(-, {..o})))"),
        )
    }

    @Test
    fun `reject conflicting owners when merging boards`() = runTest {
        val error = assertFailsWith<HexoNotationException> { TCHNotationParser.parse($$"$merge({x}, {o})") }

        assertFalse(error is HexoNotationFormatException)
        assertContains(error.message, "Owner override")
    }

    @Test
    fun `reject unknown functions without parsing their arguments`() = runTest {
        val error = assertFailsWith<HexoNotationException> {
            $$"$unknown({x})".parseTCHNotation(NotationParser.Companion.None)
        }

        assertFalse(error is HexoNotationFormatException)
        assertContains(error.message, "Unknown function")
    }

    @Test
    fun `reject missing function arguments`() = runTest {
        for (notation in listOf($$"$rotate()", $$"$rotate(1)", $$"$mirror()", $$"$mirror(-)")) {
            val error = assertFailsWith<HexoNotationException>(notation) {
                notation.parseTCHNotation(NotationParser.Companion.None)
            }

            assertFalse(error is HexoNotationFormatException, notation)
            assertContains(error.message, "Missing argument", message = notation)
        }
    }

    @Test
    fun `reject arguments of the wrong type`() = runTest {
        for ((notation, message) in listOf(
            $$"$rotate(/, {x})" to "Expected integer at 1 for 'rotate'",
            $$"$rotate({x}, {o})" to "Expected integer at 1 for 'rotate'",
            $$"$rotate(1, 2)" to "Expected board at 2 for 'rotate'",
            $$"$mirror(1, {x})" to "Expected axis at 1 for 'mirror'",
            $$"$mirror({x}, {o})" to "Expected axis at 1 for 'mirror'",
            $$"$mirror(-, /)" to "Expected board at 2 for 'mirror'",
            $$"$merge(1, {x})" to "Expected board at 1 for 'merge'",
        )) {
            val error = assertFailsWith<HexoNotationException>(notation) {
                notation.parseTCHNotation(NotationParser.Companion.None)
            }

            assertFalse(error is HexoNotationFormatException, notation)
            assertEquals(message, error.message, notation)
        }
    }

    @Test
    fun `reject trailing arguments for fixed arity functions`() = runTest {
        val parser = object : NotationParser {
            override suspend fun parse(notation: String) = Board()
        }

        for ((function, notation) in listOf(
            "rotate" to $$"$rotate(1, {x}, 2)",
            "mirror" to $$"$mirror(-, {x}, {o})",
        )) {
            val error = assertFailsWith<HexoNotationException>(notation) { notation.parseTCHNotation(parser) }

            assertFalse(error is HexoNotationFormatException, notation)
            assertEquals("Unexpected trailing arguments for function '$function'", error.message, notation)
        }
    }

    @Test
    fun `wrap embedded parser errors and retain their cause`() = runTest {
        for (cause in listOf(HexoNotationException("Invalid board"), HexoNotationFormatException("Unknown notation"))) {
            val parser = object : NotationParser {
                override suspend fun parse(notation: String): Board = throw cause
            }
            val error = assertFailsWith<HexoNotationException> { $$"${invalid}".parseTCHNotation(parser) }

            assertIsNot<HexoNotationFormatException>(error)
            assertEquals("Invalid expression notation: ${cause.message}", error.message)
            assertSame(cause, error.cause)
        }
    }

    @Test
    fun `unrelated notation is a format mismatch`() = runTest {
        for (notation in listOf("", "unrelated text", "x.o", "rotate(1, {x})", "{x}")) {
            assertFailsWith<HexoNotationFormatException>(notation) {
                notation.parseTCHNotation(NotationParser.Companion.None)
            }
        }
    }

    @Test
    fun `reject malformed expressions before delegating to the board parser`() = runTest {
        for (notation in listOf(
            $$"$rotate(1 {x})",
            $$"$rotate(1,)",
            $$"$rotate(1, {x}",
            $$"$rotate(1, {x}) trailing",
            $$"$rotate(1, {x}) ${o}",
            $$"$rotate(1, {x)",
            $$"$mirror(<, {x})",
            $$"$rotate(1, $mirror(-, {x}))",
        )) {
            val error = assertFailsWith<HexoNotationException>(notation) {
                notation.parseTCHNotation(NotationParser.Companion.None)
            }

            assertFalse(error is HexoNotationFormatException, notation)
            assertContains(error.message, "Invalid notation at", message = notation)
        }
    }
}
