package de.mineking.hexo.board

import de.mineking.hexo.core.CellOwner
import kotlinx.serialization.Serializable

@Serializable(with = BoardSerializer::class)
interface Board {
    companion object {
        const val WIN_MIN_LENGTH = 6
    }

    val lineHighlights: List<LineHighlight>
    val cells: Map<CellCoordinate, Cell>

    val attributes: BoardAttributes
}

fun Board(): Board = MutableBoard(attributes = BoardAttributes.createDefault())

class MutableBoard(
    override val lineHighlights: MutableList<LineHighlight> = mutableListOf(),
    override val cells: MutableMap<CellCoordinate, MutableCell> = mutableMapOf(),
    override val attributes: MutableBoardAttributes = MutableBoardAttributes(),
) : Board {
    fun highlightLine(origin: CellCoordinate, direction: Direction, length: Int, color: CellOwner? = null) {
        lineHighlights += LineHighlight(origin, direction, length, color)
    }

    operator fun get(q: Int, r: Int) = get(CellCoordinate(q, r))
    operator fun get(coordinate: CellCoordinate) = cells.getOrPut(coordinate) { MutableCell() }

    operator fun set(q: Int, r: Int, cell: MutableCell) = set(CellCoordinate(q, r), cell)
    operator fun set(coordinate: CellCoordinate, cell: MutableCell) {
        cells[coordinate] = cell
    }

    override fun equals(other: Any?) = other is Board && cells == other.cells && lineHighlights == other.lineHighlights
    override fun hashCode(): Int {
        var result = lineHighlights.hashCode()
        result = 31 * result + cells.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }
}

private val directions = listOf(
    CellCoordinate(1, 0),
    CellCoordinate(0, 1),
    CellCoordinate(1, -1),
)

fun Board.findWinningRows(): List<List<Pair<CellCoordinate, Cell>>> {
    val rows = mutableListOf<List<Pair<CellCoordinate, Cell>>>()

    for ((coordinate, cell) in cells) {
        val owner = cell.owner ?: continue

        for (direction in directions) {
            val previousCoordinate = coordinate - direction
            val previousOwner = cells[previousCoordinate]?.owner
            if (previousOwner == owner) continue

            val row = mutableListOf<Pair<CellCoordinate, Cell>>()
            var current = coordinate

            while (true) {
                val currentCell = cells[current] ?: break
                if (currentCell.owner != owner) break

                row += current to currentCell
                current += direction
            }

            if (row.size >= Board.WIN_MIN_LENGTH) {
                rows += row
            }
        }
    }

    return rows
}

@IgnorableReturnValue
fun MutableBoard.focusWinningRows() = apply {
    findWinningRows().forEach {
        it.forEach { (coordinate) ->
            this[coordinate].focused = true
        }
    }
}

fun Board.copy() = MutableBoard(
    lineHighlights = this@copy.lineHighlights.toMutableList(),
    cells = this@copy.cells.mapValues { (_, cell) -> cell.copy() }.toMutableMap(),
    attributes = this@copy.attributes.copy(),
)

fun Board.mutable() = when (this) {
    is MutableBoard -> this
    else -> copy()
}

operator fun Board.plus(other: Board) = merge(other)
fun Board.merge(other: Board, overrideOwner: Boolean = false): Board {
    val cells = mutableMapOf<CellCoordinate, MutableCell>()
    this.cells.forEach { (coordinate, cell) -> cells[coordinate] = cell.copy() }

    other.cells.forEach { (coordinate, cell) ->
        cells.merge(coordinate, cell.copy()) { old, new ->
            requireHexo(overrideOwner || old.owner == null || new.owner == null) {
                "At $coordinate: Owner override is disabled but both cells have an owner defined"
            }
            old + new.toOverride()
        }
    }
    return MutableBoard(
        cells = cells,
        lineHighlights = (this@merge.lineHighlights + other.lineHighlights).toMutableList(),
        attributes = this@merge.attributes + other.attributes,
    )
}

@IgnorableReturnValue
private fun <K, V> MutableMap<K, V>.merge(key: K, value: V, merge: (V, V) -> V?): V? {
    val old = this[key]
    val new = if (old == null) value else merge(old, value)

    if (new == null) {
        this -= key
    } else {
        this[key] = new
    }

    return new
}
