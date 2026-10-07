package de.mineking.hexo.board.binary

internal object SymbolCodec {
    private enum class Mode { Uniform, Packed, Sparse }

    fun write(output: BitWriter, values: IntArray, radix: Int) {
        require(values.isNotEmpty() && radix in 2..4)

        val encoding = if (values.all { it == values[0] }) {
            encodeUniform(values[0], radix)
        } else {
            var best = encodePacked(values, radix)
            for (default in 0 until radix) best = smallestEncoding(best, encodeSparse(values, radix, default))
            best
        }
        output.append(encoding)
    }

    fun read(input: BitReader, count: Int, radix: Int): IntArray = when (input.readEnum<Mode>()) {
        Mode.Uniform -> {
            val value = input.readBits(indexBitWidth(radix))
            require(value < radix) { "Invalid uniform value" }
            IntArray(count) { value }
        }
        Mode.Packed -> RadixPacking.read(input, count, radix)
        Mode.Sparse -> readSparse(input, count, radix)
    }

    private fun encodeUniform(value: Int, radix: Int) = BitWriter().apply {
        writeEnum(Mode.Uniform)
        writeBits(value, indexBitWidth(radix))
    }

    private fun encodePacked(values: IntArray, radix: Int) = BitWriter().apply {
        writeEnum(Mode.Packed)
        RadixPacking.write(this, values, radix)
    }

    private fun encodeSparse(values: IntArray, radix: Int, default: Int) = BitWriter().apply {
        writeEnum(Mode.Sparse)
        writeBits(default, indexBitWidth(radix))
        val indices = values.indices.filter { values[it] != default }
        writePositive(indices.size.toLong())

        var previous = -1
        for (index in indices) {
            writePositive((index - previous).toLong())
            previous = index
        }

        val exceptions = IntArray(indices.size) {
            val value = values[indices[it]]
            if (value < default) value else value - 1
        }
        RadixPacking.write(this, exceptions, radix - 1)
    }

    private fun readSparse(input: BitReader, count: Int, radix: Int): IntArray {
        val default = input.readBits(indexBitWidth(radix))
        require(default < radix) { "Invalid default value" }

        val exceptionCount = input.readPositive(count.toLong()).toInt()
        input.requireBits(exceptionCount.toLong() * Byte.SIZE_BITS, "Invalid exception count")

        val indices = IntArray(exceptionCount)
        var previous = -1
        for (i in indices.indices) {
            val gap = input.readPositive((count - previous - 1).toLong())
            previous = (previous + gap).toInt()
            indices[i] = previous
        }

        val exceptions = RadixPacking.read(input, exceptionCount, radix - 1)
        return IntArray(count) { default }.also { result ->
            for (i in indices.indices) {
                val value = exceptions[i]
                result[indices[i]] = if (value < default) value else value + 1
            }
        }
    }
}

private object RadixPacking {
    fun write(output: BitWriter, values: IntArray, radix: Int) {
        if (radix == 1) return
        val blockSize = blockSize(radix)
        var index = 0
        while (index < values.size) {
            val end = minOf(index + blockSize, values.size)
            var power = 1
            var value = 0
            for (i in index until end) {
                value += values[i] * power
                power *= radix
            }
            output.writeBits(value, indexBitWidth(power))
            index = end
        }
    }

    fun read(input: BitReader, count: Int, radix: Int): IntArray {
        if (radix == 1) return IntArray(count)
        val blockSize = blockSize(radix)
        val fullBlockBits = indexBitWidth(power(radix, blockSize))
        val tailBits = indexBitWidth(power(radix, count % blockSize))
        input.requireBits((count / blockSize).toLong() * fullBlockBits + tailBits, "Invalid packed value count")
        val result = IntArray(count)
        var index = 0
        while (index < count) {
            val end = minOf(index + blockSize, count)
            val alphabetSize = power(radix, end - index)
            var value = input.readBits(indexBitWidth(alphabetSize))
            require(value < alphabetSize) { "Invalid packed value" }
            for (i in index until end) {
                result[i] = value % radix
                value /= radix
            }
            index = end
        }
        return result
    }

    private fun blockSize(radix: Int): Int {
        var size = 1
        var power = radix
        while (power * radix <= 256) {
            power *= radix
            size++
        }
        return size
    }

    private fun power(radix: Int, exponent: Int): Int {
        var result = 1
        repeat(exponent) { result *= radix }
        return result
    }
}
