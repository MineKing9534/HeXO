package de.mineking.hexo.board.binary

import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.BoardAttributes
import de.mineking.hexo.board.MutableBoardAttributes

internal class BoardHeader private constructor(
    private val showTurnNumbers: StoredBoolean,
    val hasCells: Boolean,
    val hasLines: Boolean,
) {
    fun writeTo(output: BitWriter) {
        output.writeBits(VERSION, VERSION_BITS)
        output.writeBoolean(hasCells)
        output.writeBoolean(hasLines)
        output.writeEnum(showTurnNumbers)
    }

    fun applyTo(attributes: MutableBoardAttributes) {
        when (showTurnNumbers) {
            StoredBoolean.Absent -> Unit
            StoredBoolean.False -> attributes[BoardAttribute.ShowTurnNumbers] = false
            StoredBoolean.True -> attributes[BoardAttribute.ShowTurnNumbers] = true
        }
    }

    companion object {
        private const val VERSION = 1
        private const val VERSION_BITS = 3

        fun create(attributes: BoardAttributes, hasCells: Boolean, hasLines: Boolean): BoardHeader {
            require(attributes.values.keys.all { it == BoardAttribute.ShowTurnNumbers }) { "Unsupported board attribute" }
            val setting = when (attributes.values[BoardAttribute.ShowTurnNumbers]) {
                null -> StoredBoolean.Absent
                false -> StoredBoolean.False
                true -> StoredBoolean.True
                else -> throw IllegalArgumentException("Invalid ShowTurnNumbers value")
            }
            return BoardHeader(setting, hasCells, hasLines)
        }

        fun readFrom(input: BitReader): BoardHeader {
            require(input.readBits(VERSION_BITS) == VERSION) { "Unsupported BoardBinary version" }
            val hasCells = input.readBoolean()
            val hasLines = input.readBoolean()
            val setting = input.readEnum<StoredBoolean>()
            return BoardHeader(setting, hasCells, hasLines)
        }
    }

    private enum class StoredBoolean { Absent, False, True }
}
