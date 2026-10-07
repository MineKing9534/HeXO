package de.mineking.hexo.board.binary

import okio.Buffer
import kotlin.enums.enumEntries

internal const val MAX_UNSIGNED = 0x1ffffffffL
private const val VARINT_PAYLOAD_BITS = 7
private const val VARINT_PAYLOAD_MASK = 0x7f
private const val VARINT_CONTINUATION = 0x80
private const val MAX_VARINT_BYTES = 5

internal class BitWriter {
    private val buffer = Buffer()
    private var pendingByte = 0
    private var pendingBits = 0
    var bitSize = 0L
        private set

    fun writeBits(value: Int, width: Int) {
        require(width in 0..Int.SIZE_BITS)
        requireCapacity(width.toLong())
        var shift = 0
        while (shift < width) {
            val chunk = minOf(Byte.SIZE_BITS - pendingBits, width - shift)
            val mask = (1 shl chunk) - 1
            pendingByte = pendingByte or (((value ushr shift) and mask) shl pendingBits)
            pendingBits += chunk
            bitSize += chunk
            shift += chunk
            if (pendingBits == Byte.SIZE_BITS) {
                buffer.writeByte(pendingByte)
                pendingByte = 0
                pendingBits = 0
            }
        }
    }

    inline fun <reified E : Enum<E>> writeEnum(value: E) = writeBits(value.ordinal, indexBitWidth(enumEntries<E>().size))

    fun writeBoolean(value: Boolean) = writeBits(if (value) 1 else 0, 1)

    fun writeByte(value: Int) = writeBits(value, Byte.SIZE_BITS)

    fun writeUnsigned(value: Long) {
        require(value in 0..MAX_UNSIGNED) { "Unsigned integer out of range" }

        var remaining = value
        do {
            val payload = (remaining and VARINT_PAYLOAD_MASK.toLong()).toInt()
            remaining = remaining ushr VARINT_PAYLOAD_BITS
            writeByte(payload or if (remaining != 0L) VARINT_CONTINUATION else 0)
        } while (remaining != 0L)
    }

    fun writePositive(value: Long) {
        require(value in 1..MAX_UNSIGNED + 1) { "Positive integer out of range" }
        writeUnsigned(value - 1)
    }

    fun writeSigned(value: Long) = writeUnsigned((value shl 1) xor (value shr 63))

    fun append(other: BitWriter) {
        requireCapacity(other.bitSize)
        val source = other.buffer.copy()
        val tail = other.pendingByte
        val tailBits = other.pendingBits

        if (pendingBits == 0) {
            val size = source.size
            buffer.write(source, size)
            bitSize += size * Byte.SIZE_BITS
        } else {
            while (!source.exhausted()) writeByte(source.readByte().toInt() and 0xff)
        }

        writeBits(tail, tailBits)
    }

    fun toByteArray(): ByteArray = buffer.copy().apply {
        if (pendingBits != 0) writeByte(pendingByte)
    }.readByteArray()

    private fun requireCapacity(additionalBits: Long) {
        require(bitSize + additionalBits <= Int.MAX_VALUE.toLong() * Byte.SIZE_BITS) { "BoardBinary payload too large" }
    }
}

internal class BitReader(private val buffer: ByteArray) {
    private var position = 0L
    val remainingBits get() = buffer.size.toLong() * Byte.SIZE_BITS - position

    fun readBits(width: Int): Int {
        require(width in 0..Int.SIZE_BITS)
        requireBits(width.toLong(), "Truncated BoardBinary payload")
        var value = 0
        var shift = 0
        while (shift < width) {
            val offset = position.toInt() and 7
            val chunk = minOf(Byte.SIZE_BITS - offset, width - shift)
            val mask = (1 shl chunk) - 1
            val bits = (buffer[(position ushr 3).toInt()].toInt() ushr offset) and mask
            value = value or (bits shl shift)
            position += chunk
            shift += chunk
        }
        return value
    }

    inline fun <reified E : Enum<E>> readEnum(): E {
        val entries = enumEntries<E>()
        val ordinal = readBits(indexBitWidth(entries.size))
        require(ordinal < entries.size) { "Invalid enum ordinal: $ordinal" }
        return entries[ordinal]
    }

    fun readBoolean() = readBits(1) != 0

    fun readByte() = readBits(Byte.SIZE_BITS)

    fun readUnsigned(max: Long = MAX_UNSIGNED): Long {
        var value = 0L
        repeat(MAX_VARINT_BYTES) { index ->
            val byte = readByte()
            val payload = byte and VARINT_PAYLOAD_MASK
            value = value or (payload.toLong() shl (index * VARINT_PAYLOAD_BITS))
            if (byte and VARINT_CONTINUATION == 0) {
                require(index == 0 || payload != 0) { "Overlong integer" }
                require(value <= max) { "Integer exceeds allowed range" }
                return value
            }
        }
        throw IllegalArgumentException("Unsigned integer overflow")
    }

    fun readPositive(max: Long = MAX_UNSIGNED + 1): Long {
        require(max in 1..MAX_UNSIGNED + 1) { "Invalid positive integer limit" }
        return readUnsigned(max - 1) + 1
    }

    fun readSigned(): Long {
        val value = readUnsigned()
        return (value ushr 1) xor -(value and 1)
    }

    fun readSignedInt() = checkedInt(readSigned())

    fun requireBits(count: Long, message: String) {
        require(count in 0..remainingBits) { message }
    }

    fun finish() {
        require(remainingBits < Byte.SIZE_BITS && readBits(remainingBits.toInt()) == 0) {
            "Trailing data or nonzero padding"
        }
    }
}

internal fun checkedInt(value: Long): Int {
    require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "Integer overflow" }
    return value.toInt()
}

internal fun indexBitWidth(size: Int): Int {
    require(size > 0)
    return Int.SIZE_BITS - (size - 1).countLeadingZeroBits()
}

internal fun smallestEncoding(vararg candidates: BitWriter): BitWriter = candidates.minBy { it.bitSize }
