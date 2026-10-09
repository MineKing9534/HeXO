package de.mineking.hexo.board.binary

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.isEmpty
import kotlin.io.encoding.Base64

object BoardBinary {
    fun encode(board: Board): ByteArray {
        val entries = board.cells.entries
            .filter { !it.value.isEmpty(includeHighlights = true) }
            .sortedWith(compareBy({ it.key.r }, { it.key.q }))

        val header = BoardHeader.create(
            attributes = board.attributes,
            hasCells = entries.isNotEmpty(),
            hasLines = board.lineHighlights.isNotEmpty(),
        )
        val output = BitWriter()
        header.writeTo(output)
        if (header.hasCells) {
            output.writePositive(entries.size.toLong())
            output.append(CoordinateCodec.encode(entries.map { it.key }))
            CellCodec.write(output, entries.map { it.value })
        }
        if (header.hasLines) LineCodec.write(output, board.lineHighlights)
        return output.toByteArray()
    }

    fun encodeToString(board: Board) = Base64.UrlSafe.encode(encode(board))

    fun decode(
        bytes: ByteArray,
        maxCells: Int = 5000,
        maxLines: Int = 30,
        maxLabelBytes: Int = 1024,
    ): Board {
        val input = BitReader(bytes)
        val header = BoardHeader.readFrom(input)
        val board = MutableBoard()
        header.applyTo(board.attributes)

        if (header.hasCells) {
            // A uniform rectangle can describe many cells with very few bytes.
            // Check the allocation limit before expanding coordinates or columns.
            val count = input.readPositive(maxCells.toLong()).toInt()
            val coordinates = CoordinateCodec.read(input, count)
            val cells = CellCodec.read(input, count, maxLabelBytes)
            for (i in coordinates.indices) board.cells[coordinates[i]] = cells[i]
        }
        if (header.hasLines) board.lineHighlights += LineCodec.read(input, maxLines)
        input.finish()
        return board
    }

    fun decodeFromString(
        encoded: String,
        maxCells: Int = 5000,
        maxLines: Int = 30,
        maxLabelBytes: Int = 1024,
    ) = decode(Base64.UrlSafe.decode(encoded), maxCells, maxLines, maxLabelBytes)
}
