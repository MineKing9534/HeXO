package de.mineking.hexo.board.parse.test

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
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
            version[2];
            1. [0,1][1,-1];
            2. [1,0][-1,0]<0,1:#:$A1><-2,1:$EMPTY2><0,0:#X><3,-2:#O><9,9>;
        """.trimIndent().parseHTTTXNotation()

        assertEquals(Cell(CellOwner.O, highlight = CellHighlight(null), turn = 1, label = "A1"), board.cells[CellCoordinate(1, -1)])
        assertEquals(Cell(label = "EMPTY2"), board.cells[CellCoordinate(-1, -1)])
        assertEquals(Cell(CellOwner.X, highlight = CellHighlight(CellOwner.X), turn = 0), board.cells[CellCoordinate.Zero])
        assertEquals(Cell(highlight = CellHighlight(CellOwner.O)), board.cells[CellCoordinate(1, 2)])
        assertEquals(Cell(highlight = CellHighlight(null)), board.cells[CellCoordinate(18, -9)])
        assertEquals(8, board.cells.size)
    }

    @Test
    fun `combine visuals for the same coordinate in order`() {
        val board = $$"version[2]; 1. [1,0]<0,0:$FIRST><0,0:#X><0,0:$SECOND>;".parseHTTTXNotation()

        assertEquals(Cell(CellOwner.X, highlight = CellHighlight(CellOwner.X), turn = 0, label = "SECOND"), board.cells[CellCoordinate.Zero])
    }

    @Test
    fun `reject malformed visuals`() {
        for (visual in listOf("<0,0:>", "<0,0:$>", "<0,0:#AB>", "<0:#>", "<0,0:#")) {
            assertFailsWith<HexoNotationException>(visual) {
                "version[2]; 1. [1,0]$visual;".parseHTTTXNotation()
            }
        }
    }

    @Test
    fun `reject visual coordinates outside the supported range`() {
        for (coordinate in listOf("2147483648,0", "2147483647,1", "0,-2147483648")) {
            assertFailsWith<HexoNotationException>(coordinate) {
                "version[2]; 1. [1,0]<$coordinate:#>;".parseHTTTXNotation()
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
        val e = assertFailsWith<HexoNotationException> {
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

    @Test
    fun `parse supported version extensions`() {
        val cells = mapOf(
            CellCoordinate.Zero to Cell(CellOwner.X, turn = 0),
            CellCoordinate(1, 0) to Cell(CellOwner.O, turn = 1),
        )

        val board = "version[2u]; 1. [1,0];".parseHTTTXNotation()

        assertEquals(cells, board.cells)
    }

    @Test
    fun `reject extensions in version 1`() {
        for (extensions in listOf("u", "uu", "x", "uz")) {
            val e = assertFailsWith<HexoNotationException>(extensions) {
                "version[1$extensions]; 1. [1,0];".parseHTTTXNotation()
            }

            assertEquals("Extensions are only supported in HTTTX version 2 and onward", e.message)
        }
    }

    @Test
    fun `reject duplicate version extensions`() {
        for (extensions in listOf("uu", "uuu")) {
            val e = assertFailsWith<HexoNotationException>(extensions) {
                "version[2$extensions]; 1. [1,0];".parseHTTTXNotation()
            }

            assertEquals("Duplicate HTTTX extension 'u'", e.message)
        }
    }

    @Test
    fun `reject whitespace inside version tags`() {
        for (whitespace in listOf(" ", "\t", "\r", "\n")) {
            for (value in listOf("${whitespace}2u", "2${whitespace}u", "2u$whitespace", "2u${whitespace}x", "2$whitespace")) {
                val e = assertFailsWith<HexoNotationException>(value) {
                    "version[$value]; 1. [1,0];".parseHTTTXNotation()
                }

                assertEquals("Whitespace is not allowed in HTTTX version tags", e.message)
            }
        }
    }

    @Test
    fun `allow whitespace around version tags`() {
        val board = " \tversion[2u]\n;\r\n1. [1,0]; ".parseHTTTXNotation()

        assertEquals(
            mapOf(
                CellCoordinate.Zero to Cell(CellOwner.X, turn = 0),
                CellCoordinate(1, 0) to Cell(CellOwner.O, turn = 1),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse version extensions with metadata and setup`() {
        val board = "version[2u] u[enabled] x[first] o[second]; x[0,1]:o;".parseHTTTXNotation()

        assertEquals(mapOf(CellCoordinate(1, -1) to Cell(CellOwner.X)), board.cells)
    }

    @Test
    fun `reject unknown version extensions`() {
        for ((extensions, unknown) in listOf(
            "z" to 'z',
            "x" to 'x',
            "o" to 'o',
            "X" to 'X',
            "O" to 'O',
            "U" to 'U',
            "uz" to 'z',
            "ux" to 'x',
            "uXo" to 'X',
        )) {
            val e = assertFailsWith<HexoNotationException>(extensions) {
                "version[2$extensions]; 1. [1,0];".parseHTTTXNotation()
            }

            assertEquals("Unknown HTTTX extension '$unknown'", e.message)
        }
    }

    @Test
    fun `reject malformed version extensions`() {
        for (extensions in listOf("-", "u1", "_", "u?")) {
            val e = assertFailsWith<HexoNotationException>(extensions) {
                "version[2$extensions]; 1. [1,0];".parseHTTTXNotation()
            }

            assertContains(e.message, "Invalid notation at")
        }
    }

    @Test
    fun `parse setup with player names as metadata keys`() {
        val board = "version[2] x[first] o[second] owner[name]; x:o; 1. [0,0];".parseHTTTXNotation()

        assertEquals(mapOf(CellCoordinate.Zero to Cell(CellOwner.X, turn = 1)), board.cells)
    }

    @Test
    fun `parse setup without turns`() {
        val board = """
            version[2];
            x[0,1][1,-1]:o[-2,1][3,-2];
        """.trimIndent().parseHTTTXNotation()

        assertEquals(
            mapOf(
                CellCoordinate(1, -1) to Cell(CellOwner.X),
                CellCoordinate(0, 1) to Cell(CellOwner.X),
                CellCoordinate(-1, -1) to Cell(CellOwner.O),
                CellCoordinate(1, 2) to Cell(CellOwner.O),
            ),
            board.cells,
        )
    }

    @Test
    fun `parse setup with empty player positions`() {
        val setups = mapOf(
            "x:o;" to emptyMap(),
            "o:x;" to emptyMap(),
            "x[0,0]:o;" to mapOf(CellCoordinate.Zero to Cell(CellOwner.X)),
            "x:o[0,0];" to mapOf(CellCoordinate.Zero to Cell(CellOwner.O)),
        )

        for ((setup, cells) in setups) {
            assertEquals(cells, "version[2]; $setup".parseHTTTXNotation().cells, setup)
        }
    }

    @Test
    fun `first setup player moves first and subsequent turns alternate`() {
        for ((setup, firstPlayer) in listOf("x[0,1]:o[-2,1];" to CellOwner.X, "o[0,1]:x[-2,1];" to CellOwner.O)) {
            val board = """
                version[2];
                $setup
                1. [0,0][1,0];
                2. [2,0][3,0];
                3. [4,0];
            """.trimIndent().parseHTTTXNotation()

            assertEquals(
                mapOf(
                    CellCoordinate(1, -1) to Cell(firstPlayer),
                    CellCoordinate(-1, -1) to Cell(firstPlayer.other),
                    CellCoordinate.Zero to Cell(firstPlayer, turn = 1),
                    CellCoordinate(1, 0) to Cell(firstPlayer, turn = 1),
                    CellCoordinate(2, 0) to Cell(firstPlayer.other, turn = 2),
                    CellCoordinate(3, 0) to Cell(firstPlayer.other, turn = 2),
                    CellCoordinate(4, 0) to Cell(firstPlayer, turn = 3),
                ),
                board.cells,
                setup,
            )
        }
    }

    @Test
    fun `apply setup visuals without turns`() {
        val board = $$"""
            version[2];
            x[0,1]:o<0,1:#X:$STONE><-2,1:#:$EMPTY>;
        """.trimIndent().parseHTTTXNotation()

        assertEquals(
            mapOf(
                CellCoordinate(1, -1) to Cell(CellOwner.X, highlight = CellHighlight(CellOwner.X), label = "STONE"),
                CellCoordinate(-1, -1) to Cell(highlight = CellHighlight(null), label = "EMPTY"),
            ),
            board.cells,
        )
    }

    @Test
    fun `last move visuals update setup visuals and preserve unrelated ones`() {
        val board = $$"""
            version[2];
            x[0,1]:o[-2,1]<0,1:#X:$OLD><-2,1:#:$KEEP>;
            1. [1,0]<0,1:#O:$NEW>;
        """.trimIndent().parseHTTTXNotation()

        assertEquals(
            mapOf(
                CellCoordinate(1, -1) to Cell(CellOwner.X, highlight = CellHighlight(CellOwner.O), label = "NEW"),
                CellCoordinate(-1, -1) to Cell(CellOwner.O, highlight = CellHighlight(null), label = "KEEP"),
                CellCoordinate(1, 0) to Cell(CellOwner.X, turn = 1),
            ),
            board.cells,
        )
    }

    @Test
    fun `reject setup in version 1`() {
        val e = assertFailsWith<HexoNotationException> {
            "version[1]; x[0,0]:o; 1. [1,0];".parseHTTTXNotation()
        }

        assertEquals("Setup is only supported in HTTTX version 2 and onward", e.message)
    }

    @Test
    fun `reject setup defining the same player twice`() {
        for (setup in listOf("x:x;", "o:o;", "x[0,0]:x[1,0];", "o[0,0]:o[1,0];")) {
            val e = assertFailsWith<HexoNotationException>(setup) {
                "version[2]; $setup 1. [2,0];".parseHTTTXNotation()
            }

            assertEquals("HTTTX setup cannot define the same player twice", e.message)
        }
    }

    @Test
    fun `reject repeated coordinates within a setup player`() {
        for (setup in listOf("x[0,1][0,1]:o;", "x:o[0,1][0,1];", "o[0,1][0,1]:x;", "o:x[0,1][0,1];")) {
            val e = assertFailsWith<HexoNotationException>(setup) {
                "version[2]; $setup".parseHTTTXNotation()
            }

            assertEquals("Duplicate HTTTX setup cell at ${CellCoordinate(1, -1)}", e.message)
        }
    }

    @Test
    fun `reject setup players occupying the same coordinate`() {
        for (setup in listOf("x[0,1]:o[0,1];", "o[0,1]:x[0,1];")) {
            val e = assertFailsWith<HexoNotationException>(setup) {
                "version[2]; $setup".parseHTTTXNotation()
            }

            assertEquals("Duplicate HTTTX setup cell at ${CellCoordinate(1, -1)}", e.message)
        }
    }

    @Test
    fun `reject moves on coordinates occupied by either setup player`() {
        for (move in listOf("[0,1]", "[-2,1]")) {
            val e = assertFailsWith<HexoNotationException>(move) {
                "version[2]; x[0,1]:o[-2,1]; 1. $move;".parseHTTTXNotation()
            }

            assertContains(e.message, "Duplicate HTTTX move")
        }
    }

    @Test
    fun `reject malformed setup`() {
        for (setup in listOf("x[0,0];", "x[0,0]o[1,0];", "x[0]:o;", "x[0,0]:o:x;")) {
            assertFailsWith<HexoNotationException>(setup) {
                "version[2]; $setup 1. [2,0];".parseHTTTXNotation()
            }
        }
    }
}
