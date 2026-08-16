package de.mineking.hexo.board

import kotlinx.serialization.Serializable

@RequiresOptIn
annotation class InternalBoardApi

@Serializable(with = BoardSerializer::class)
interface Board {
    companion object {
        const val WIN_MIN_LENGTH = 6

        fun withTurnNumbers() = Board(attributes = BoardAttributes(BoardAttribute.ShowTurnNumbers to true))
    }

    val lineHighlights: List<LineHighlight>
    val cells: Map<CellCoordinate, Cell>

    val attributes: BoardAttributes
}

fun Board(
    lineHighlights: List<LineHighlight> = listOf(),
    cells: Map<CellCoordinate, Cell> = mapOf(),
    attributes: BoardAttributes = BoardAttributes(),
): Board = MutableBoard(
    lineHighlights = lineHighlights.toMutableList(),
    cells = cells.mapValues { (_, cell) -> cell.copy() }.toMutableMap(),
    attributes = attributes.copy(),
)

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

    override fun equals(other: Any?) = other is Board &&
        cells == other.cells &&
        lineHighlights == other.lineHighlights &&
        other.attributes == attributes

    override fun hashCode(): Int {
        var result = lineHighlights.hashCode()
        result = 31 * result + cells.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }
}

fun Board.hasHighlights() = lineHighlights.isNotEmpty() || cells.values.any { it.highlight != null }
fun Board.isEmpty(includeHighlights: Boolean): Boolean {
    if (includeHighlights && lineHighlights.isNotEmpty()) return false
    return cells.all { (_, cell) -> cell.isEmpty(includeHighlights) }
}

fun Board.findWinningRows(requiredLength: Int = Board.WIN_MIN_LENGTH): List<BoardLine> {
    val result = mutableListOf<BoardLine>()

    for ((coordinate, cell) in cells) {
        val owner = cell.owner ?: continue

        for (direction in Direction.entries.take(3)) {
            val previousCoordinate = coordinate - direction.direction
            val previousOwner = cells[previousCoordinate]?.owner
            if (previousOwner == owner) continue

            var current = coordinate
            var length = 0

            while (true) {
                val currentCell = cells[current] ?: break
                if (currentCell.owner != owner) break

                current += direction.direction
                length++
            }

            if (length >= requiredLength) {
                result += BoardLine(coordinate, direction, length)
            }
        }
    }

    return result
}

@IgnorableReturnValue
fun MutableBoard.focusWinningRows() = apply {
    findWinningRows().forEach {
        it.forEach { coordinate ->
            this[coordinate].focused = true
        }
    }
}

fun Board.copy() = MutableBoard(
    lineHighlights = this@copy.lineHighlights.toMutableList(),
    cells = this@copy.cells.mapValues { (_, cell) -> cell.copy() }.toMutableMap(),
    attributes = this@copy.attributes.copy(),
)

@InternalBoardApi
fun Board.mutable() = when (this) {
    is MutableBoard -> this
    else -> copy()
}

operator fun Board.plus(other: Board) = merge(other)
fun Board.merge(other: Board, overrideOwner: Boolean = false): MutableBoard {
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
