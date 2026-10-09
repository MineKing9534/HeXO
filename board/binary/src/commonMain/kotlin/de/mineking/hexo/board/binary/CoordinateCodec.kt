package de.mineking.hexo.board.binary

import de.mineking.hexo.board.CellCoordinate

internal object CoordinateCodec {
    private enum class Mode { Sparse, Dense }
    private enum class SparseMode { Deltas, Rows }

    fun encode(coordinates: List<CellCoordinate>): BitWriter {
        require(coordinates.isNotEmpty())
        val sparse = smallestEncoding(encodeDeltas(coordinates), encodeRows(coordinates))
        val dense = encodeRectangle(coordinates, sparse.bitSize)

        return if (dense != null && dense.bitSize < sparse.bitSize) {
            dense
        } else {
            sparse
        }
    }

    fun read(input: BitReader, count: Int): List<CellCoordinate> = when (input.readEnum<Mode>()) {
        Mode.Sparse -> {
            val mode = input.readEnum<SparseMode>()
            input.requireBits(count.toLong() * Byte.SIZE_BITS, "Invalid coordinate count")
            when (mode) {
                SparseMode.Deltas -> readDeltas(input, count)
                SparseMode.Rows -> readRows(input, count)
            }
        }
        Mode.Dense -> readRectangle(input, count)
    }

    private fun encodeDeltas(coordinates: List<CellCoordinate>) = BitWriter().apply {
        writeEnum(Mode.Sparse)
        writeEnum(SparseMode.Deltas)
        var previous = CellCoordinate.Zero
        for (coordinate in coordinates) {
            writeCoordinateDelta(coordinate, previous)
            previous = coordinate
        }
    }

    private fun readDeltas(input: BitReader, count: Int): List<CellCoordinate> {
        val result = ArrayList<CellCoordinate>(count)
        var previous = CellCoordinate.Zero

        repeat(count) {
            val coordinate = input.readCoordinateDelta(previous)
            require(result.isEmpty() ||
                coordinate.r > previous.r ||
                (coordinate.r == previous.r && coordinate.q > previous.q),
            ) { "Coordinates are not strictly ordered" }
            result += coordinate
            previous = coordinate
        }
        return result
    }

    private fun encodeRows(coordinates: List<CellCoordinate>) = BitWriter().apply {
        writeEnum(Mode.Sparse)
        writeEnum(SparseMode.Rows)
        var index = 0
        var previousR = 0

        while (index < coordinates.size) {
            val first = coordinates[index]

            var end = index + 1
            while (end < coordinates.size && coordinates[end].r == first.r) end++

            if (index == 0) {
                writeSigned(first.r.toLong())
            } else {
                writePositive(first.r.toLong() - previousR)
            }

            writePositive((end - index).toLong())
            writeSigned(first.q.toLong())

            for (i in index + 1 until end) writePositive(coordinates[i].q.toLong() - coordinates[i - 1].q)

            previousR = first.r
            index = end
        }
    }

    private fun readRows(input: BitReader, count: Int): List<CellCoordinate> {
        val result = ArrayList<CellCoordinate>(count)
        var previousR = 0

        while (result.size < count) {
            val r = if (result.isEmpty()) {
                input.readSignedInt()
            } else {
                checkedInt(previousR.toLong() + input.readPositive())
            }

            val rowCount = input.readPositive((count - result.size).toLong()).toInt()
            var q = input.readSignedInt()
            result += CellCoordinate(q, r)

            repeat(rowCount - 1) {
                q = checkedInt(q.toLong() + input.readPositive())
                result += CellCoordinate(q, r)
            }
            previousR = r
        }
        return result
    }

    private fun encodeRectangle(coordinates: List<CellCoordinate>, sparseBits: Long): BitWriter? {
        val rectangle = Rectangle.enclosing(coordinates) ?: return null
        val full = rectangle.area == coordinates.size.toLong()
        // Never materialize a bitmap that cannot beat the sparse candidate.
        if (!full && rectangle.area >= sparseBits) return null

        return BitWriter().apply {
            writeEnum(Mode.Dense)
            rectangle.writeTo(this)
            writeBoolean(full)

            if (full) return@apply

            var position = 0L
            for (coordinate in coordinates) {
                val next = rectangle.indexOf(coordinate)
                while (position < next) {
                    writeBoolean(false)
                    position++
                }
                writeBoolean(true)
                position++
            }
            while (position < rectangle.area) {
                writeBoolean(false)
                position++
            }
        }
    }

    private fun readRectangle(input: BitReader, count: Int): List<CellCoordinate> {
        val rectangle = Rectangle.readFrom(input)
        val full = input.readBoolean()
        require(rectangle.area >= count) { "Invalid coordinate rectangle" }

        if (full) {
            require(rectangle.area == count.toLong()) { "Invalid coordinate rectangle" }
        } else {
            input.requireBits(rectangle.area, "Invalid coordinate rectangle")
        }

        val result = ArrayList<CellCoordinate>(count)
        var position = 0L

        while (position < rectangle.area) {
            if (full || input.readBoolean()) {
                require(result.size < count) { "Too many coordinates" }
                result += rectangle.coordinateAt(position)
            }
            position++
        }

        require(result.size == count) { "Coordinate count mismatch" }
        return result
    }

    private class Rectangle(val minQ: Int, val minR: Int, val width: Long, val height: Long) {
        val area = width * height

        fun indexOf(coordinate: CellCoordinate) = (coordinate.r.toLong() - minR) * width + coordinate.q.toLong() - minQ
        fun coordinateAt(index: Long) = CellCoordinate((minQ + index % width).toInt(), (minR + index / width).toInt())

        fun writeTo(output: BitWriter) {
            output.writeSigned(minQ.toLong())
            output.writeSigned(minR.toLong())
            output.writePositive(width)
            output.writePositive(height)
        }

        companion object {
            private const val MAX_DIMENSION = 0x100000000L

            fun enclosing(coordinates: List<CellCoordinate>): Rectangle? {
                val minQ = coordinates.minOf { it.q }
                val minR = coordinates.first().r
                val width = coordinates.maxOf { it.q }.toLong() - minQ + 1
                val height = coordinates.last().r.toLong() - minR + 1

                if (height > Long.MAX_VALUE / width) return null
                return Rectangle(minQ, minR, width, height)
            }

            fun readFrom(input: BitReader): Rectangle {
                val minQ = input.readSignedInt()
                val minR = input.readSignedInt()
                val width = input.readPositive(MAX_DIMENSION)
                val height = input.readPositive(MAX_DIMENSION)

                require(minQ.toLong() + width - 1 <= Int.MAX_VALUE && minR.toLong() + height - 1 <= Int.MAX_VALUE) {
                    "Coordinate rectangle exceeds integer range"
                }
                require(height <= Long.MAX_VALUE / width) { "Coordinate rectangle overflow" }

                return Rectangle(minQ, minR, width, height)
            }
        }
    }
}

internal fun BitWriter.writeCoordinateDelta(coordinate: CellCoordinate, previous: CellCoordinate) {
    writeSigned(coordinate.q.toLong() - previous.q)
    writeSigned(coordinate.r.toLong() - previous.r)
}

internal fun BitReader.readCoordinateDelta(previous: CellCoordinate) = CellCoordinate(
    checkedInt(previous.q.toLong() + readSigned()),
    checkedInt(previous.r.toLong() + readSigned()),
)
