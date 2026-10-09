package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.MutableCell
import de.mineking.hexo.board.parse.notation.parseHTTTXNotation
import de.mineking.hexo.board.render.notation.renderHTTTXNotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HTTTXNotationRenderTest {
    private fun assertRoundTrip(board: Board, notation: String = board.renderHTTTXNotation()) {
        assertEquals(board.cells, notation.parseHTTTXNotation().cells)
    }

    @Test
    fun `empty board renders only the version header and empty state`() {
        val board = Board()
        val notation = board.renderHTTTXNotation()

        assertEquals("version[2];\nx : o;\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `two move turns render version one and preserve coordinates`() {
        val board = MutableBoard().apply {
            this[0, 0] = MutableCell(CellOwner.X, turn = 0)
            this[2, -1] = MutableCell(CellOwner.O, turn = 1)
            this[-3, 2] = MutableCell(CellOwner.O, turn = 1)
            this[1, 2] = MutableCell(CellOwner.X, turn = 2)
            this[-2, -1] = MutableCell(CellOwner.X, turn = 2)
        }
        val notation = board.renderHTTTXNotation()

        assertEquals("version[1];\n1. [1,1][-1,-2];\n2. [3,-2][-3,1];\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `nonstandard turn sizes require the unlimited extension`() {
        for (moves in listOf(1, 3, 4)) {
            val board = MutableBoard().apply {
                this[0, 0] = MutableCell(CellOwner.X, turn = 0)
                for (q in 1..moves) {
                    this[q, 0] = MutableCell(CellOwner.O, turn = 1)
                }
            }
            val coordinates = (1..moves).joinToString("") { "[$it,0]" }
            val notation = board.renderHTTTXNotation()

            assertEquals("version[2u];\n1. $coordinates;\n", notation)
            assertRoundTrip(board, notation)
        }
    }

    @Test
    fun `a partial final turn upgrades otherwise standard turns`() {
        val board = "version[2u]; 1. [1,0][2,0]; 2. [-1,0];".parseHTTTXNotation()
        val notation = board.renderHTTTXNotation()

        assertEquals("version[2u];\n1. [1,0][2,0];\n2. [-1,0];\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `turns are sorted and renumbered consecutively`() {
        val board = MutableBoard().apply {
            this[-1, 0] = MutableCell(CellOwner.X, turn = 40)
            this[-2, 0] = MutableCell(CellOwner.X, turn = 40)
            this[1, 0] = MutableCell(CellOwner.O, turn = 20)
            this[2, 0] = MutableCell(CellOwner.O, turn = 20)
            this[0, 0] = MutableCell(CellOwner.X, turn = 10)
        }
        val notation = board.renderHTTTXNotation()

        assertEquals("version[1];\n1. [1,0][2,0];\n2. [-1,0][-2,0];\n", notation)
        assertEquals(
            "version[1]; 1. [1,0][2,0]; 2. [-1,0][-2,0];".parseHTTTXNotation().cells,
            notation.parseHTTTXNotation().cells,
        )
    }

    @Test
    fun `setup without turns uses version two and starts with x`() {
        val board = MutableBoard().apply {
            this[2, -1].owner = CellOwner.X
            this[-3, 2].owner = CellOwner.O
        }
        val notation = board.renderHTTTXNotation()

        assertEquals("version[2];\nx [1,1] : o [-1,-2];\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `setup includes an empty player without adding an implicit origin`() {
        val board = MutableBoard().apply {
            this[1, 0].owner = CellOwner.O
        }
        val notation = board.renderHTTTXNotation()

        assertEquals("version[2];\nx : o [1,0];\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `setup player order follows the first turn owner`() {
        for (owner in CellOwner.entries) {
            val board = MutableBoard().apply {
                this[0, 0].owner = CellOwner.X
                this[-1, 0].owner = CellOwner.O
                this[1, 0] = MutableCell(owner, turn = 1)
                this[2, 0] = MutableCell(owner, turn = 1)
            }
            val setup = if (owner == CellOwner.X) "x [0,0] : o [-1,0]" else "o [-1,0] : x [0,0]"
            val notation = board.renderHTTTXNotation()

            assertEquals("version[2];\n$setup;\n1. [1,0][2,0];\n", notation)
            assertRoundTrip(board, notation)
        }
    }

    @Test
    fun `setup and nonstandard turns require version two with the unlimited extension`() {
        val board = MutableBoard().apply {
            this[0, 0].owner = CellOwner.X
            this[1, 0] = MutableCell(CellOwner.O, turn = 1)
        }
        val notation = board.renderHTTTXNotation()

        assertEquals("version[2u];\no : x [0,0];\n1. [1,0];\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `highlight only cells render neutral and player colored visuals`() {
        for (color in listOf(null, CellOwner.X, CellOwner.O)) {
            val board = MutableBoard().apply {
                this[2, -1].highlight = CellHighlight(color)
            }
            val notation = board.renderHTTTXNotation()

            assertEquals("version[2];\nx : o <1,1:#${color?.symbol ?: ""}>;\n", notation)
            assertRoundTrip(board, notation)
        }
    }

    @Test
    fun `label only cells render setup visuals`() {
        val board = MutableBoard().apply {
            this[-3, 2].label = "keep"
        }
        val notation = board.renderHTTTXNotation()

        assertEquals($$"version[2];\nx : o <-1,-2:$keep>;\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `label rendering preserves whitespace`() {
        val board = MutableBoard().apply {
            this[1, 0].label = "keep this"
        }

        assertEquals($$"version[2];\nx : o <1,0:$keep this>;\n", board.renderHTTTXNotation())
    }

    @Test
    fun `highlights and labels share a visual on occupied setup cells`() {
        val board = MutableBoard().apply {
            this[2, -1] = MutableCell(CellOwner.O, highlight = CellHighlight(CellOwner.X), label = "note")
        }
        val notation = board.renderHTTTXNotation()

        assertEquals($$"version[2];\nx : o [1,1] <1,1:#x:$note>;\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `labels escape HTTTX special characters`() {
        val board = MutableBoard().apply {
            this[1, 0].label = $$"a>b:c#d$e"
        }
        val notation = board.renderHTTTXNotation()

        assertEquals($$"version[2];\nx : o <1,0:$a\\>b\\:c\\#d\\$e>;\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `visual only setup is retained alongside turns`() {
        val board = MutableBoard().apply {
            this[-1, 0] = MutableCell(highlight = CellHighlight(null), label = "keep")
            this[1, 0] = MutableCell(CellOwner.O, turn = 1)
            this[2, 0] = MutableCell(CellOwner.O, turn = 1)
        }
        val notation = board.renderHTTTXNotation()

        assertEquals($$"version[2];\no : x <-1,0:#:$keep>;\n1. [1,0][2,0];\n", notation)
        assertRoundTrip(board, notation)
    }

    @Test
    fun `custom line separators apply to the header setup and turns`() {
        val board = "version[2]; x[0,0]:o; 1. [1,0][2,0];".parseHTTTXNotation()

        for (separator in listOf("\r\n", " ", "")) {
            val notation = board.renderHTTTXNotation(separator)

            assertEquals("version[2];${separator}x [0,0] : o;${separator}1. [1,0][2,0];$separator", notation)
            assertRoundTrip(board, notation)
        }
    }

    @Test
    fun `mixed owners in a turn are rejected`() {
        val board = MutableBoard().apply {
            this[1, 0] = MutableCell(CellOwner.X, turn = 1)
            this[2, 0] = MutableCell(CellOwner.O, turn = 1)
        }

        assertFailsWith<IllegalArgumentException> { board.renderHTTTXNotation() }
    }

    @Test
    fun `consecutive turns from the same owner are rejected`() {
        val board = MutableBoard().apply {
            this[1, 0] = MutableCell(CellOwner.X, turn = 1)
            this[2, 0] = MutableCell(CellOwner.X, turn = 2)
        }

        assertFailsWith<IllegalArgumentException> { board.renderHTTTXNotation() }
    }
}
