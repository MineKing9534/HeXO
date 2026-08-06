package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.parse.parseBKENotation
import de.mineking.hexo.board.parse.parseHTTTXNotation
import de.mineking.hexo.board.parse.parseTytoNotation
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import kotlin.test.assertTrue

abstract class AbstractImageRendererTest(private val extension: String, private val renderer: BoardRenderer<Theme, ByteArray>) {
    protected fun test(name: String, board: Board, theme: DefaultTheme) = runTest {
        val actual = renderer.render(board, theme.theme)
        val themeName = theme.name.lowercase()
        val snapshotPath = "$extension/$themeName/$name.$extension"
        val expected = javaClass.getResourceAsStream("/$snapshotPath")?.readAllBytes()

        if (!actual.contentEquals(expected)) {
            val file = File("actual/$snapshotPath")
            file.parentFile.mkdirs()

            file.outputStream().use { it.write(actual) }
        }
        assertTrue(actual.contentEquals(expected))
    }

    @EnumSource
    @ParameterizedTest
    fun `longsword htttx`(theme: DefaultTheme) {
        val board = """
            version[1];
            1. [0,1][1,-1];
            2. [1,0][-1,0];
            3. [2,0][-4,0];
        """.trimIndent().parseHTTTXNotation().copy()

        board[-4, 0].highlight = CellHighlight(null)
        board[-3, 0].highlight = CellHighlight(null)

        test("longsword_htttx", board, theme)
    }

    @EnumSource
    @ParameterizedTest
    fun `longsword BKE`(theme: DefaultTheme) {
        val board = "o A0 A2 x A1 A4 o B1.0 D4.0".trimIndent().parseBKENotation(
            origin = null,
            zeroOffsetLine = Direction.Right,
        ).copy()

        test("longsword_bke", board, theme)
    }

    @EnumSource
    @ParameterizedTest
    fun `labels and highlights`(theme: DefaultTheme) {
        val board = MutableBoard()
        board[0, 0].apply {
            owner = CellOwner.X
            highlight = CellHighlight(null)
        }
        board[3, 0].apply {
            owner = CellOwner.X
            focused = true
        }
        board[0, 1].apply {
            owner = CellOwner.O
            label = "1"
        }
        board[2, 1].apply {
            owner = CellOwner.O
            highlight = CellHighlight(CellOwner.X)
        }
        board[0, 3].owner = CellOwner.X
        board[0, 4].apply {
            highlight = CellHighlight(null)
            label = "b"
        }
        board[-1, 4].label = "a"

        test("labels", board, theme)
    }

    @EnumSource
    @ParameterizedTest
    fun `line highlight test`(theme: DefaultTheme) {
        val board = MutableBoard()
        board[0, 1].apply {
            owner = CellOwner.X
            label = "a"
        }
        board[0, 2].owner = CellOwner.O
        board.highlightLine(CellCoordinate(1, 0), Direction.Left, length = 4, color = CellOwner.X)
        board.highlightLine(CellCoordinate(0, 2), Direction.TopLeft, length = 5, color = null)

        test("line_highlight", board, theme)
    }

    @EnumSource
    @ParameterizedTest
    fun `game test`(theme: DefaultTheme) {
        val board = "AgQEBgYBBAICCAIGBQYFEAIKBgYCAAIMBAgIBAEOCgIGBAQEAQQKBAgCCAYBBgEMCgAMAQ".parseTytoNotation()

        test("game", board, theme)
    }

    @EnumSource
    @ParameterizedTest
    fun `long label test`(theme: DefaultTheme) {
        val board = MutableBoard().apply { this[0, 0].label = "meow meow" }
        test("long_label", board, theme)
    }

    @Test
    fun `label color test`() {
        val board = MutableBoard().apply { this[0, 0].label = "#00ff00 a" }
        test("label_color", board, DefaultTheme.HDS)
    }
}
