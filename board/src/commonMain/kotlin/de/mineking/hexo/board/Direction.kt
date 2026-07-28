package de.mineking.hexo.board

import kotlinx.serialization.Serializable

@Serializable
enum class Direction(val symbol: String, val direction: CellCoordinate) {
    Right(">", CellCoordinate(1, 0)),
    BottomRight("q", CellCoordinate(0, 1)),
    BottomLeft("p", CellCoordinate(-1, 1)),
    Left("<", CellCoordinate(-1, 0)),
    TopLeft("b", CellCoordinate(0, -1)),
    TopRight("d", CellCoordinate(1, -1)),
    ;

    companion object {
        fun fromSymbol(symbol: String) = entries.firstOrNull { it.symbol == symbol }
            ?: throw HexoNotationException("Unknown symbol: $symbol. Valid symbols are ${entries.joinToString { "`${it.symbol}`" }}")
    }

    fun ringDirection(sector: Int) = entries[(ordinal + sector + 2) % entries.size].direction
}
