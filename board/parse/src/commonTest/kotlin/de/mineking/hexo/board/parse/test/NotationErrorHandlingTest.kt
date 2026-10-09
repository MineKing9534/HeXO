package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.HexoNotationFormatException
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.parse.notation.BKENotationParser
import de.mineking.hexo.board.parse.notation.CombinedNotationParser
import de.mineking.hexo.board.parse.notation.HTTTXNotationParser
import de.mineking.hexo.board.parse.notation.RectilinearNotationParser
import de.mineking.hexo.board.parse.notation.TytoLinkParser
import de.mineking.hexo.board.parse.or
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class NotationErrorHandlingTest {
    @Test
    fun `syntax errors in recognizable notation are not format mismatches`() = runTest {
        for ((parser, inputs) in listOf(
            HTTTXNotationParser to listOf(
                "version[1]; 1. [1,0]",
                "version[1]; 1. [1,?];",
                "version[1]; 1. [1,0]<0,0:#AB>;",
                "version[",
                "version[1]; 1. [1,?]; 2. [2,0][3,0]; 3. [4,0][5,0];",
                "version[?]; 1. [1,0]; 2. [2,0][3,0]; 3. [4,0][5,0];",
            ),
            RectilinearNotationParser to listOf("x.[ab", "x(?)", "x/?", "x ?"),
            BKENotationParser to listOf("x A0 ?", "x", "d CW x", "d x A0", "0 x A0"),
        )) {
            for (input in inputs) {
                val e = assertFailsWith<HexoNotationException>(input) { parser.parse(" \n$input") }
                assertFalse(e is HexoNotationFormatException, input)
                assertContains(e.message, "Invalid notation at")
            }
        }
    }

    @Test
    fun `empty boards with board structure retain their semantic error`() = runTest {
        for (input in listOf(".", "c.", "-/")) {
            val e = assertFailsWith<HexoNotationException>(input) { RectilinearNotationParser.parse(input) }
            assertFalse(e is HexoNotationFormatException, input)
            assertEquals("Cannot parse an empty board", e.message)
        }
    }

    @Test
    fun `column notation commits after its marker and cell`() = runTest {
        val e = assertFailsWith<HexoNotationException> { RectilinearNotationParser.parse("cx/?") }
        assertFalse(e is HexoNotationFormatException)
    }

    @Test
    fun `unrelated inputs remain format mismatches`() = runTest {
        for (parser in listOf(HTTTXNotationParser, RectilinearNotationParser, BKENotationParser, CombinedNotationParser.Default)) {
            for (input in listOf("", "unrelated text", "https://example.com")) {
                assertFailsWith<HexoNotationFormatException>(input) { parser.parse(input) }
            }
        }
    }

    @Test
    fun `rectilinear parser yields to recognizable BKE and HTTTX notation`() = runTest {
        for (input in listOf("x A0 ?", "d CW x A0", "version[1]; 1. [1,0];")) {
            assertFailsWith<HexoNotationFormatException>(input) { RectilinearNotationParser.parse(input) }
        }
    }

    @Test
    fun `recovery and lookahead do not establish a matching prefix`() = runTest {
        for ((parser, input) in listOf(
            HTTTXNotationParser to "1. [1,0];",
            HTTTXNotationParser to "? version[1]; 1. [1,0];",
            HTTTXNotationParser to " \n ? version[1]; 1. [1,0];",
            RectilinearNotationParser to " \n ? x.o",
            BKENotationParser to "A0",
            BKENotationParser to "CW x A0",
            BKENotationParser to "? x A0",
        )) {
            assertFailsWith<HexoNotationFormatException>(input) { parser.parse(input) }
        }
    }

    @Test
    fun `lexer errors retain successful matches pending during lookahead`() = runTest {
        for (input in listOf("version[?", "version[1]; 1. [1,?];")) {
            val e = assertFailsWith<HexoNotationException>(input) { HTTTXNotationParser.parse(input) }
            assertFalse(e is HexoNotationFormatException, input)
            assertContains(e.message, "token recognition error")
        }
    }

    @Test
    fun `fallback runs only for format mismatches`() = runTest {
        var fallbackCalls = 0
        val fallback = object : NotationParser {
            override suspend fun parse(notation: String): Board {
                fallbackCalls++
                return Board()
            }
        }
        val parser = HTTTXNotationParser or fallback

        assertEquals(Board(), parser.parse("x.o"))
        assertEquals(1, fallbackCalls)
        assertFailsWith<HexoNotationException> { parser.parse("version[1]; 1. [1,?];") }
        assertEquals(1, fallbackCalls)
    }

    @Test
    fun `default parser preserves the identified parsers error`() = runTest {
        for ((parser, input) in listOf(
            HTTTXNotationParser to "version[1]; 1. [1,?];",
            RectilinearNotationParser to "1. [1,0];",
            RectilinearNotationParser to "x.[ab",
            RectilinearNotationParser to "x---?",
            RectilinearNotationParser to "x[label||text] ?",
            BKENotationParser to "x A0 ?",
            BKENotationParser to "d CW x",
            BKENotationParser to "x A6",
            CombinedNotationParser.Default to "x[*] || x A0 ?",
            TytoLinkParser to "https://hexo.tyto.cc/analysis#c=a b",
        )) {
            val expected = assertFailsWith<HexoNotationException>(input) { parser.parse(input) }
            val actual = assertFailsWith<HexoNotationException>(input) { NotationParser.Default.parse(input) }
            assertFalse(actual is HexoNotationFormatException, input)
            assertEquals(expected.message, actual.message, input)
        }
    }

    @Test
    fun `combined notation remains identified with malformed components`() = runTest {
        for (input in listOf("x[*] || unrelated", "x[*] || x A0 ?")) {
            val e = assertFailsWith<HexoNotationException>(input) { CombinedNotationParser.Default.parse(input) }
            assertFalse(e is HexoNotationFormatException, input)
        }
    }

    @Test
    fun `malformed combined structure is a format mismatch`() = runTest {
        for (input in listOf("x[*] || x A0 || x A1", "x[*] ||", "|| x A0", "x||?")) {
            assertFailsWith<HexoNotationFormatException>(input) { CombinedNotationParser.Default.parse(input) }
            assertFailsWith<HexoNotationFormatException>(input) { NotationParser.Default.parse(input) }
        }
    }

    @Test
    fun `default parser still accepts each notation format`() = runTest {
        for ((parser, input) in listOf(
            HTTTXNotationParser to "version[1]; 1. [1,0];",
            RectilinearNotationParser to "x.o",
            RectilinearNotationParser to "x O",
            RectilinearNotationParser to "x---x",
            RectilinearNotationParser to "x[label||text]",
            RectilinearNotationParser to "x[*]---x",
            BKENotationParser to "x A0",
            BKENotationParser to "d CW o A0",
            CombinedNotationParser.Default to ".[*] || x A0",
            TytoLinkParser to "https://hexo.tyto.cc/analysis#c=",
        )) {
            assertEquals(parser.parse(input), NotationParser.Default.parse(input), input)
        }
        assertEquals(mapOf(CellCoordinate.Zero to Cell(CellOwner.X, turn = 0)), NotationParser.Default.parse("0").cells)
    }
}
