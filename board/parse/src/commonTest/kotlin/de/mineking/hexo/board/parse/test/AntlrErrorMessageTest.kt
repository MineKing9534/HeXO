package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.parse.generated.RectilinearLexer
import de.mineking.hexo.board.parse.notation.BKENotationParser
import de.mineking.hexo.board.parse.notation.HTTTXNotationParser
import de.mineking.hexo.board.parse.notation.RectilinearNotationParser
import de.mineking.hexo.board.parse.notation.formatExpectedTokenNames
import kotlinx.coroutines.test.runTest
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.misc.IntervalSet
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AntlrErrorMessageTest {
    @Test
    fun `missing token names are lowercase`() = runTest {
        val e = assertFailsWith<HexoNotationException> {
            HTTTXNotationParser.parse("version[]; 1. [1,0];")
        }

        assertContains(e.message, "missing integer")
        assertFalse(e.message.contains("INTEGER"))
    }

    @Test
    fun `expected token names are lowercase without changing offending input`() = runTest {
        val e = assertFailsWith<HexoNotationException> { BKENotationParser.parse("x CW") }

        assertContains(e.message, "'CW'")
        assertContains(e.message, "expecting move")
        assertFalse(e.message.contains("MOVE"))
    }

    @Test
    fun `context token names are lowercase without changing coordinates`() = runTest {
        val e = assertFailsWith<HexoNotationException> { BKENotationParser.parse("d A0") }

        assertContains(e.message, "'A0'")
        assertContains(e.message, "chirality")
        assertFalse(e.message.contains("CHIRALITY"))
    }

    @Test
    fun `expected token sets use lowercase names`() = runTest {
        val e = assertFailsWith<HexoNotationException> { RectilinearNotationParser.parse("xo?") }

        assertContains(e.message, "owner")
        assertContains(e.message, "highlighted_owner")
        assertContains(e.message, "integer")
        assertContains(e.message, "<eof>")
        assertContains(e.message, "'!'")
        assertFalse(e.message.contains("OWNER"))
        assertFalse(e.message.contains("INTEGER"))
        assertFalse(e.message.contains("<EOF>"))
        assertFalse(e.message.contains("ws"))
    }

    @Test
    fun `whitespace is omitted without changing the expected tokens`() {
        val vocabulary = RectilinearLexer(CharStreams.fromString("")).vocabulary
        val expected = IntervalSet.of(RectilinearLexer.Tokens.OWNER).apply {
            add(RectilinearLexer.Tokens.WS)
        }

        assertEquals("owner", expected.formatExpectedTokenNames(vocabulary))
        assertEquals(listOf(RectilinearLexer.Tokens.OWNER, RectilinearLexer.Tokens.WS), expected.toList())
    }

    @Test
    fun `whitespace only expectations have a readable fallback`() {
        val vocabulary = RectilinearLexer(CharStreams.fromString("")).vocabulary
        val expected = IntervalSet.of(RectilinearLexer.Tokens.WS)

        assertEquals("whitespace", expected.formatExpectedTokenNames(vocabulary))
    }

    @Test
    fun `unexpected end of input is lowercase`() = runTest {
        val e = assertFailsWith<HexoNotationException> { BKENotationParser.parse("x") }

        assertContains(e.message, "'<eof>'")
        assertContains(e.message, "move")
    }

    @Test
    fun `lexer errors preserve the offending characters`() = runTest {
        val e = assertFailsWith<HexoNotationException> {
            HTTTXNotationParser.parse("version[2]; 1. [1,0]<0,0:#Z>;")
        }

        assertContains(e.message, "token recognition error at: 'Z'")
    }
}
