package de.mineking.hexo.board.binary

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.LineHighlight
import kotlin.enums.enumEntries

internal object LineCodec {
    private const val DIRECTION_COUNT = 6
    private const val LENGTH_STRIDE = DIRECTION_COUNT * OwnerSymbols.RADIX
    private const val VALID_CODES = LineHighlight.MAX_LENGTH * LENGTH_STRIDE
    private const val MIN_RECORD_BITS = 3 * Byte.SIZE_BITS

    fun write(output: BitWriter, lines: List<LineHighlight>) {
        output.writePositive(lines.size.toLong())
        var previous = CellCoordinate.Zero
        for (line in lines) {
            output.writeCoordinateDelta(line.start, previous)
            val code = (line.length - 1) * LENGTH_STRIDE + line.direction.ordinal * OwnerSymbols.RADIX + OwnerSymbols.encode(line.color)
            output.writeByte(code)
            previous = line.start
        }
    }

    fun read(input: BitReader, maxLines: Int): List<LineHighlight> {
        val count = input.readPositive(maxLines.toLong()).toInt()
        input.requireBits(count.toLong() * MIN_RECORD_BITS, "Invalid line highlight count")

        var previous = CellCoordinate.Zero
        return List(count) {
            val start = input.readCoordinateDelta(previous)
            val code = input.readByte()
            require(code < VALID_CODES) { "Invalid line highlight" }

            val line = LineHighlight(
                start = start,
                direction = enumEntries<Direction>()[code / OwnerSymbols.RADIX % DIRECTION_COUNT],
                length = code / LENGTH_STRIDE + 1,
                color = OwnerSymbols.decode(code % OwnerSymbols.RADIX),
            )
            previous = start
            line
        }
    }
}
