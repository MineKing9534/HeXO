package de.mineking.hexo.board

import de.mineking.hexo.core.CellOwner
import kotlinx.serialization.Serializable
import kotlin.math.abs

sealed interface Cell {
    companion object {
        val EMPTY = Cell()
    }

    val owner: CellOwner?
    val highlight: CellHighlight?
    val focused: Boolean
    val turn: Int?
    val label: String

    fun copy(): MutableCell
}

fun Cell(
    owner: CellOwner? = null,
    highlight: CellHighlight? = null,
    focused: Boolean = false,
    turn: Int? = null,
    label: String = "",
): Cell = MutableCell(owner, highlight, focused, turn, label)

data class MutableCell(
    override var owner: CellOwner? = null,
    override var highlight: CellHighlight? = null,
    override var focused: Boolean = false,
    override var turn: Int? = null,
    override var label: String = "",
) : Cell {
    override fun copy() = copy(owner = owner)
}

fun Cell.isEmpty() = this == Cell.EMPTY

@Serializable
data class CellHighlight(val color: CellOwner?)

@Serializable
data class CellCoordinate(val q: Int, val r: Int) {
    companion object {
        val Zero = CellCoordinate(0, 0)
    }

    override fun toString() = "($q, $r)"
}

fun CellCoordinate.distanceTo(other: CellCoordinate): Int {
    val ds = (q + r) - (other.q + other.r)
    return maxOf(abs(q - other.q), abs(r - other.r), abs(ds))
}

operator fun CellCoordinate.plus(other: CellCoordinate) = CellCoordinate(q + other.q, r + other.r)
operator fun CellCoordinate.minus(other: CellCoordinate) = CellCoordinate(q - other.q, r - other.r)
operator fun CellCoordinate.times(scalar: Int) = CellCoordinate(q * scalar, r * scalar)

@Serializable
data class LineHighlight(val start: CellCoordinate, val direction: Direction, val length: Int, val color: CellOwner?) {
    init {
        requireHexo(length in 1..MAX_LENGTH) { "Length must be between 1 and $MAX_LENGTH" }
    }

    companion object {
        const val MAX_LENGTH = 12
    }
}

val LineHighlight.end get() = start + direction.direction * (length - 1)
operator fun LineHighlight.contains(coordinate: CellCoordinate) = (0 until length).any { coordinate == start + direction.direction * it }
