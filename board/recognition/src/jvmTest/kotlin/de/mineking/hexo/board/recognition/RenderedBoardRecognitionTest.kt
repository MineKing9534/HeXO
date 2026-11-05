package de.mineking.hexo.board.recognition

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.render.image.renderToImage
import de.mineking.hexo.board.render.image.theme.BaseTheme
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.HDSTheme
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.asOutputStream
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RenderedBoardRecognitionTest {
    @ParameterizedTest
    @EnumSource
    fun `round trip across built-in themes`(theme: DefaultTheme) {
        assertRoundTrip(sampleBoard(), theme.theme)
    }

    @ParameterizedTest
    @EnumSource
    fun `round trip with labels and turns`(theme: DefaultTheme) {
        val board = sampleBoard().apply {
            cells.values.forEachIndexed { index, cell ->
                cell.turn = index + 1
                cell.label = "${index + 10}"
            }
        }
        assertRoundTrip(board, theme.theme)
    }

    @ParameterizedTest
    @EnumSource
    fun `round trip after downscaling`(theme: DefaultTheme) {
        assertRoundTrip(
            sampleBoard(),
            theme.theme,
            layoutRadius = 90.0,
            options = BoardRecognitionOptions(maxImageDimension = 320),
        )
    }

    @Test
    fun `round trip with an unfamiliar palette`() {
        val theme = HDSTheme.Default.copy(
            backgroundColor = Color.rgb(0xf3ede0),
            cellBorderColor = Color.rgb(0x877c68),
            playerXColor = Color.rgb(0x493388),
            playerOColor = Color.rgb(0x40c468),
        )
        assertRoundTrip(sampleBoard(), theme, layoutRadius = 28.5)
    }

    @Test
    fun `encoded image source is recognized at the byte limit`() = runTest {
        val image = sampleBoard().renderToImage(36.0, padding = 45, visibleRadius = 2, theme = HDSTheme.Default)
        val source = Buffer()
        ImageIO.write(image, "png", source.asOutputStream())
        val recognizer = LocalBoardRecognizer(maxImageBytes = source.size, maxImagePixels = 16_000_000L)

        val actual = recognizer.recognizeBoard(source)
        assertEquals(image.recognizeBoard().board, actual.board)
    }

    @Test
    fun `encoded image source enforces the byte limit`() = runTest {
        val source = Buffer().apply { write(ByteArray(9)) }
        val recognizer = LocalBoardRecognizer(maxImageBytes = 8, maxImagePixels = 16_000_000L)

        val error = assertFailsWith<BoardRecognitionException> { recognizer.recognizeBoard(source) }
        assertEquals("Image too large", error.message)
    }

    @Test
    fun `unsupported image source is rejected`() = runTest {
        val source = Buffer().apply { write(byteArrayOf(1, 2, 3)) }
        assertFailsWith<IOException> { LocalBoardRecognizer.Default.recognizeBoard(source) }
    }

    private fun assertRoundTrip(
        board: Board,
        theme: BaseTheme,
        layoutRadius: Double = 36.0,
        options: BoardRecognitionOptions = BoardRecognitionOptions(),
    ) {
        val image = board.renderToImage(layoutRadius, padding = 45, visibleRadius = 2, theme = theme)
        val recognized = image.recognizeBoard(options).board
        val expected = board.normalizedOwners()
        val actual = recognized.normalizedOwners()

        // Recognition chooses its own origin and assigns owners by brightness, not the source player names.
        val swappedOwners = expected.mapValues { (_, owner) -> owner?.other }
        assertTrue(actual == expected || actual == swappedOwners, "Expected $expected (or swapped owners), got $actual")
    }

    private fun Board.normalizedOwners(): Map<CellCoordinate, CellOwner?> {
        val minQ = cells.keys.minOfOrNull { it.q } ?: 0
        val minR = cells.keys.minOfOrNull { it.r } ?: 0
        return cells.entries.associate { (coordinate, cell) ->
            CellCoordinate(coordinate.q - minQ, coordinate.r - minR) to cell.owner
        }
    }

    private fun sampleBoard() = MutableBoard().apply {
        this[-2, 0].owner = CellOwner.X
        this[-1, 0].owner = CellOwner.O
        this[0, 0].owner = CellOwner.X
        this[0, 1].owner = CellOwner.O
        this[1, 1].owner = CellOwner.X
        this[-2, 2].owner = CellOwner.O
        this[2, -1].owner = CellOwner.O
        this[2, 2].owner = CellOwner.X
    }
}
