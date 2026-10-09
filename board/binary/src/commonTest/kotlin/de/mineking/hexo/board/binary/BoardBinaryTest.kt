package de.mineking.hexo.board.binary

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.BoardAttributes
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.MutableCell
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.to
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BoardBinaryTest {
    @Test
    fun headersAndSingleCellHaveStableBytes() {
        val attributes = listOf(
            BoardAttributes(),
            BoardAttributes(BoardAttribute.ShowTurnNumbers to null),
            BoardAttributes(BoardAttribute.ShowTurnNumbers to false),
            BoardAttributes(BoardAttribute.ShowTurnNumbers to true),
        )
        attributes.forEachIndexed { index, value ->
            val board = Board(attributes = value)
            val setting = (index - 1).coerceAtLeast(0)
            assertContentEquals(byteArrayOf((1 or (setting shl 5)).toByte()), BoardBinary.encode(board))
            assertRoundTrip(board)

            for (hasCells in listOf(false, true)) {
                for (hasLines in listOf(false, true)) {
                    val withSections = Board(
                        cells = if (hasCells) mapOf(CellCoordinate.Zero to Cell(CellOwner.X)) else emptyMap(),
                        lineHighlights = if (hasLines) listOf(LineHighlight(CellCoordinate.Zero, Direction.Right, 1, null)) else emptyList(),
                        attributes = value,
                    )
                    val expectedHeader = 1 or (if (hasCells) 8 else 0) or (if (hasLines) 16 else 0) or (setting shl 5)
                    assertEquals(expectedHeader, BoardBinary.encode(withSections).first().toInt() and 0x7f)
                    assertRoundTrip(withSections)
                }
            }
        }
        val single = Board(cells = mapOf(CellCoordinate.Zero to Cell(CellOwner.X)))

        assertContentEquals(byteArrayOf(0x09, 0, 0, 0, 8), BoardBinary.encode(single))
        assertRoundTrip(single)
        assertEquals(emptyMap(), BoardBinary.decode(byteArrayOf(0x01), maxCells = 0, maxLines = 0, maxLabelBytes = 0).cells)
    }

    @Test
    fun boardRoundTripsMetadataLinesAndIntegerExtremesWhileFilteringEmptyCells() {
        val board = sampleBoard()
        assertRoundTrip(board)

        val decoded = BoardBinary.decode(BoardBinary.encode(board))
        assertEquals(4, decoded.cells.size)
        assertTrue(decoded.cells.values.none { it.focused })
        val unfocused = board.copy()
        unfocused.cells.values.forEach { it.focused = false }
        assertContentEquals(BoardBinary.encode(board), BoardBinary.encode(unfocused))

        val reordered = Board(
            cells = board.cells.entries.reversed().associate { it.toPair() },
            lineHighlights = board.lineHighlights,
            attributes = board.attributes,
        )
        assertContentEquals(BoardBinary.encode(board), BoardBinary.encode(reordered))
    }

    @Test
    fun coordinateModesAreSelfContainedAndCompact() {
        val bitmap = List(9) { CellCoordinate(it % 3, it / 3) }.filter { it != CellCoordinate(1, 1) }
        val cases = listOf(
            Triple(listOf(CellCoordinate.Zero, CellCoordinate(1_000, 1_000)), listOf(0, 0), 9),
            Triple(List(10) { CellCoordinate(it * 128, 0) }, listOf(0, 1), 15),
            Triple(List(128) { CellCoordinate(it, 0) }, listOf(1), 7),
            Triple(bitmap, listOf(1), 9),
        )

        for ((coordinates, tags, size) in cases) {
            val board = Board(cells = coordinates.associateWith { Cell(CellOwner.X) })
            val bytes = BoardBinary.encode(board)
            assertEquals(size, bytes.size)

            val input = BitReader(bytes)

            assertTrue(BoardHeader.readFrom(input).hasCells)
            assertEquals(coordinates.size.toLong(), input.readPositive())

            for (tag in tags) assertEquals(tag, input.readBits(1))
            assertRoundTrip(board)
        }
    }

    @Test
    fun repeatedUtf8LabelsAndNearbyTurnsAreCompact() {
        val label = "漢😀".repeat(16)
        val board = MutableBoard().apply {
            repeat(32) { this[it, 0] = MutableCell(CellOwner.X, turn = 1_000_000 + it, label = label) }
        }
        val bytes = BoardBinary.encode(board)
        val labelBytes = label.encodeToByteArray().size

        assertTrue(bytes.size < labelBytes * 2)
        assertEquals(board, BoardBinary.decode(bytes, maxLabelBytes = labelBytes))
        assertFailsWith<IllegalArgumentException> {
            BoardBinary.decode(bytes, maxLabelBytes = labelBytes - 1)
        }
        assertRoundTrip(board)
    }

    @Test
    fun utf8UsesDefaultReplacement() {
        val label = "a" + charArrayOf('\uD800').concatToString() + "b😀"
        val board = Board(cells = mapOf(CellCoordinate.Zero to Cell(CellOwner.X, label = label)))
        val decoded = BoardBinary.decode(BoardBinary.encode(board))

        assertEquals(label.encodeToByteArray().decodeToString(), decoded.cells[CellCoordinate.Zero]?.label)

        val malformed = BitWriter().apply {
            writeBoolean(false) // Direct labels.
            writePositive(1)
            writeByte(255)
        }

        val input = BitReader(malformed.toByteArray())
        assertEquals(listOf(byteArrayOf(-1).decodeToString()), LabelCodec.read(input, 1, 1))
        input.finish()
    }

    @Test
    fun malformedInputAndAllocationLimitsAreRejected() {
        val bytes = BoardBinary.encode(sampleBoard())
        val overlongCount = BitWriter().apply {
            BoardHeader.create(BoardAttributes(), hasCells = true, hasLines = false).writeTo(this)
            writeByte(0x81)
            writeByte(0)
        }.toByteArray()
        val malformed = listOf(
            byteArrayOf(),
            byteArrayOf(0),
            byteArrayOf(0x02),
            byteArrayOf(0x61), // Reserved turn-display value.
            byteArrayOf(0x81.toByte()),
            byteArrayOf(0x01, 0),
            overlongCount,
            bytes.copyOf(bytes.size - 1),
        )
        for (input in malformed) assertFailsWith<IllegalArgumentException> { BoardBinary.decode(input) }
        assertFailsWith<IllegalArgumentException> { BoardBinary.decode(bytes, maxCells = 0) }
        assertFailsWith<IllegalArgumentException> { BoardBinary.decode(bytes, maxLines = 0) }
        assertFailsWith<IllegalArgumentException> { BoardBinary.decode(bytes, maxLabelBytes = 0) }
    }

    private fun sampleBoard(): Board {
        val line = LineHighlight(CellCoordinate(Int.MIN_VALUE, Int.MAX_VALUE), Direction.Right, 12, CellOwner.X)
        return Board(
            cells = mapOf(
                CellCoordinate(Int.MIN_VALUE, Int.MIN_VALUE) to Cell(
                    CellOwner.X, CellHighlight(CellOwner.O), focused = true, turn = Int.MIN_VALUE, label = "a",
                ),
                CellCoordinate.Zero to Cell(CellOwner.O, turn = 0, label = "漢😀"),
                CellCoordinate(2, 0) to Cell(highlight = CellHighlight(null)),
                CellCoordinate(Int.MAX_VALUE, Int.MAX_VALUE) to Cell(CellOwner.X, focused = true, turn = Int.MAX_VALUE, label = "ASCII"),
                CellCoordinate(-5, 0) to Cell(),
                CellCoordinate(-4, 0) to Cell(focused = true, turn = 12, label = " \t"),
            ),
            lineHighlights = listOf(
                line,
                LineHighlight(CellCoordinate.Zero, Direction.BottomLeft, 6, CellOwner.O),
                line,
                LineHighlight(CellCoordinate(Int.MAX_VALUE, Int.MIN_VALUE), Direction.TopRight, 1, null),
            ),
            attributes = BoardAttributes(BoardAttribute.ShowTurnNumbers to true),
        )
    }

    private fun assertRoundTrip(board: Board) {
        val before = board.copy()
        val encoded = BoardBinary.encode(board)
        val decoded = BoardBinary.decode(encoded)

        val expectedCells = board.cells
            .filterValues { !it.isEmpty(includeHighlights = true) }
            .mapValues { (_, cell) -> cell.copy().apply { focused = false } }
        assertEquals(expectedCells, decoded.cells)
        assertEquals(board.lineHighlights, decoded.lineHighlights)
        assertEquals(board.attributes.values.filterValues { it != null }, decoded.attributes.values)
        assertEquals(before, board)
        assertContentEquals(encoded, BoardBinary.encode(decoded))
    }
}
