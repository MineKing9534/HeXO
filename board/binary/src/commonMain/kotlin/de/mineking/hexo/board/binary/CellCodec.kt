package de.mineking.hexo.board.binary

import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.MutableCell

internal object CellCodec {
    private const val BOOLEAN_RADIX = 2
    private const val HIGHLIGHT_RADIX = 4

    fun write(output: BitWriter, cells: List<Cell>) {
        SymbolCodec.write(output, IntArray(cells.size) { OwnerSymbols.encode(cells[it].owner) }, OwnerSymbols.RADIX)
        val metadata = Metadata(
            highlights = cells.any { it.highlight != null },
            turns = cells.any { it.turn != null },
            labels = cells.any { it.label.isNotEmpty() },
        )

        metadata.writeTo(output)
        if (metadata.highlights) {
            val highlights = IntArray(cells.size) {
                cells[it].highlight
                    ?.let { highlight -> OwnerSymbols.encode(highlight.color) + 1 }
                    ?: 0
            }
            SymbolCodec.write(output, highlights, HIGHLIGHT_RADIX)
        }
        if (metadata.turns) writeOptional(output, cells.map { it.turn }, TurnCodec::write)
        if (metadata.labels) writeOptional(output, cells.map { it.label.takeIf(String::isNotEmpty) }, LabelCodec::write)
    }

    fun read(input: BitReader, count: Int, maxLabelBytes: Int): List<MutableCell> {
        val owners = SymbolCodec.read(input, count, OwnerSymbols.RADIX)
        val cells = List(count) { MutableCell(owner = OwnerSymbols.decode(owners[it])) }

        val metadata = Metadata.readFrom(input)
        if (metadata.highlights) {
            val highlights = SymbolCodec.read(input, count, HIGHLIGHT_RADIX)
            for (i in cells.indices) {
                if (highlights[i] != 0) {
                    cells[i].highlight = CellHighlight(OwnerSymbols.decode(highlights[i] - 1))
                }
            }
        }
        if (metadata.turns) {
            readOptional(input, cells, readValues = TurnCodec::read) { cell, turn ->
                cell.turn = turn
            }
        }

        if (metadata.labels) {
            readOptional(input, cells, readValues = { source, size -> LabelCodec.read(source, size, maxLabelBytes) }) { cell, label ->
                cell.label = label
            }
        }
        return cells
    }

    private fun <T : Any> writeOptional(output: BitWriter, values: List<T?>, writeValues: (BitWriter, List<T>) -> Unit) {
        SymbolCodec.write(output, IntArray(values.size) { if (values[it] != null) 1 else 0 }, BOOLEAN_RADIX)
        writeValues(output, values.filterNotNull())
    }

    private fun <T> readOptional(
        input: BitReader,
        cells: List<MutableCell>,
        readValues: (BitReader, Int) -> List<T>,
        applyValue: (MutableCell, T) -> Unit,
    ) {
        val present = SymbolCodec.read(input, cells.size, BOOLEAN_RADIX)
        val values = readValues(input, present.count { it != 0 })

        var index = 0
        for (i in cells.indices) {
            if (present[i] != 0) {
                applyValue(cells[i], values[index++])
            }
        }
    }

    private data class Metadata(val highlights: Boolean, val turns: Boolean, val labels: Boolean) {
        fun writeTo(output: BitWriter) {
            output.writeBoolean(highlights)
            output.writeBoolean(turns)
            output.writeBoolean(labels)
        }

        companion object {
            fun readFrom(input: BitReader) = Metadata(
                highlights = input.readBoolean(),
                turns = input.readBoolean(),
                labels = input.readBoolean(),
            )
        }
    }
}

internal object OwnerSymbols {
    const val RADIX = 3

    fun encode(owner: CellOwner?) = owner?.let { it.ordinal + 1 } ?: 0

    fun decode(symbol: Int): CellOwner? {
        require(symbol in 0 until RADIX) { "Invalid owner" }
        return if (symbol == 0) null else CellOwner.entries[symbol - 1]
    }
}
