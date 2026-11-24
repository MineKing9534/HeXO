package de.mineking.hexo.board.binary

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.binary.internal.BitReader
import de.mineking.hexo.board.binary.internal.BitWriter
import de.mineking.hexo.board.binary.internal.BoardHeader
import de.mineking.hexo.board.binary.internal.CellCodec
import de.mineking.hexo.board.binary.internal.CoordinateCodec
import de.mineking.hexo.board.binary.internal.LineCodec
import de.mineking.hexo.board.isEmpty
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class EncodedBoard(val bytes: ByteArray) {
    override fun toString() = STRING_ENCODER.encode(bytes)

    companion object {
        val STRING_ENCODER = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

        fun fromString(encoded: String): EncodedBoard {
            val bytes = try {
                STRING_ENCODER.decode(encoded)
            } catch (e: IllegalArgumentException) {
                throw HexoNotationException(e.message ?: "Invalid base64", e)
            }
            return EncodedBoard(bytes)
        }
    }
}

fun EncodedBoard.decode(
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

fun Board.encodeBinary(): EncodedBoard {
    val entries = cells.entries
        .filter { !it.value.isEmpty(includeHighlights = true) }
        .sortedWith(compareBy({ it.key.r }, { it.key.q }))

    val header = BoardHeader.create(
        attributes = attributes,
        hasCells = entries.isNotEmpty(),
        hasLines = lineHighlights.isNotEmpty(),
    )
    val output = BitWriter()
    header.writeTo(output)
    if (header.hasCells) {
        output.writePositive(entries.size.toLong())
        output.append(CoordinateCodec.encode(entries.map { it.key }))
        CellCodec.write(output, entries.map { it.value })
    }
    if (header.hasLines) LineCodec.write(output, lineHighlights)
    return EncodedBoard(output.toByteArray())
}
