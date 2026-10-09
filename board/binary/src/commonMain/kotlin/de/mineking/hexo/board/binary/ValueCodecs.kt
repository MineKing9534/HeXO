package de.mineking.hexo.board.binary

internal object TurnCodec {
    private enum class Mode { Absolute, Deltas }

    fun write(output: BitWriter, values: List<Int>) {
        output.append(smallestEncoding(encodeAbsolute(values), encodeDeltas(values)))
    }

    fun read(input: BitReader, count: Int): List<Int> {
        require(count > 0) { "Empty turn section" }
        return when (input.readEnum<Mode>()) {
            Mode.Absolute -> {
                input.requireBits(count.toLong() * Byte.SIZE_BITS, "Invalid turn count")
                List(count) { input.readSignedInt() }
            }
            Mode.Deltas -> {
                input.requireBits(count.toLong() * Byte.SIZE_BITS, "Invalid turn count")
                var previous = 0
                List(count) {
                    previous = checkedInt(previous.toLong() + input.readSigned())
                    previous
                }
            }
        }
    }

    private fun encodeAbsolute(values: List<Int>) = BitWriter().apply {
        writeEnum(Mode.Absolute)
        for (value in values) writeSigned(value.toLong())
    }

    private fun encodeDeltas(values: List<Int>) = BitWriter().apply {
        writeEnum(Mode.Deltas)
        var previous = 0
        for (value in values) {
            writeSigned(value.toLong() - previous)
            previous = value
        }
    }
}

internal object LabelCodec {
    private const val MIN_STRING_BITS = 2 * Byte.SIZE_BITS

    fun write(output: BitWriter, values: List<String>) {
        val direct = BitWriter().apply {
            writeBoolean(false)
            for (value in values) StringEncoding.write(this, value)
        }
        val dictionary = BitWriter().apply {
            writeBoolean(true)
            DictionaryCodec.write(this, values) { StringEncoding.write(this, it) }
        }
        output.append(smallestEncoding(direct, dictionary))
    }

    fun read(input: BitReader, count: Int, maxBytes: Int): List<String> {
        require(count > 0) { "Empty label section" }
        val strings = StringEncoding.Reader(input, maxBytes)
        return if (input.readBoolean()) {
            DictionaryCodec.read(input, count, MIN_STRING_BITS) { strings.read() }
        } else {
            input.requireBits(count.toLong() * MIN_STRING_BITS, "Invalid label count")
            List(count) { strings.read() }
        }
    }
}

private object DictionaryCodec {
    fun <T> write(output: BitWriter, values: List<T>, writeEntry: BitWriter.(T) -> Unit) {
        val entries = values.distinct()
        val indices = entries.withIndex().associate { it.value to it.index }
        output.writePositive(entries.size.toLong())

        for (entry in entries) output.writeEntry(entry)

        val width = indexBitWidth(entries.size)
        for (value in values) output.writeBits(indices.getValue(value), width)
    }

    fun <T> read(input: BitReader, count: Int, minEntryBits: Int, readEntry: BitReader.() -> T): List<T> {
        val size = input.readPositive(count.toLong()).toInt()
        input.requireBits(size.toLong() * minEntryBits, "Invalid dictionary size")

        val entries = List(size) { input.readEntry() }
        require(entries.toSet().size == size) { "Duplicate dictionary entry" }

        val width = indexBitWidth(size)
        input.requireBits(count.toLong() * width, "Invalid dictionary index count")

        return List(count) {
            val index = input.readBits(width)
            require(index < size) { "Invalid dictionary index" }
            entries[index]
        }
    }
}

private object StringEncoding {
    fun write(output: BitWriter, value: String) {
        require(value.isNotEmpty()) { "Empty label" }
        val bytes = value.encodeToByteArray()

        output.writePositive(bytes.size.toLong())
        for (byte in bytes) output.writeByte(byte.toInt() and 0xff)
    }

    class Reader(private val input: BitReader, private var remainingBytes: Int) {
        fun read(): String {
            val length = input.readPositive()
            require(length <= remainingBytes) { "Label byte limit exceeded" }

            input.requireBits(length * Byte.SIZE_BITS, "Truncated label")
            remainingBytes -= length.toInt()

            val bytes = ByteArray(length.toInt()) { input.readByte().toByte() }
            return bytes.decodeToString()
        }
    }
}
