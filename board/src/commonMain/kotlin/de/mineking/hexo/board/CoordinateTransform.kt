package de.mineking.hexo.board

interface CoordinateTransform {
    fun transform(coordinate: CellCoordinate): CellCoordinate

    class Rotation(val sectors: Int) : CoordinateTransform {
        override fun transform(coordinate: CellCoordinate): CellCoordinate {
            var result = coordinate
            repeat(sectors.mod(6)) {
                result = CellCoordinate(result.q + result.r, -result.q)
            }
            return result
        }
    }

    class Mirror(val axis: Direction) : CoordinateTransform {
        override fun transform(coordinate: CellCoordinate): CellCoordinate {
            val (dq, dr) = axis.direction
            val t = coordinate.q * (2 * dq + dr) + coordinate.r * (dq + 2 * dr)
            return CellCoordinate(
                q = t * dq - coordinate.q,
                r = t * dr - coordinate.r,
            )
        }
    }

    class Translate(val delta: CellCoordinate) : CoordinateTransform {
        override fun transform(coordinate: CellCoordinate) = coordinate + delta
    }
}

fun Board.rotate(sectors: Int) = transform(CoordinateTransform.Rotation(sectors))
fun Board.mirror(axis: Direction) = transform(CoordinateTransform.Mirror(axis))
fun Board.translate(delta: CellCoordinate) = transform(CoordinateTransform.Translate(delta))

fun GamePosition<*>.rotate(sectors: Int) = transform(CoordinateTransform.Rotation(sectors))
fun GamePosition<*>.mirror(axis: Direction) = transform(CoordinateTransform.Mirror(axis))
fun GamePosition<*>.translate(delta: CellCoordinate) = transform(CoordinateTransform.Translate(delta))

fun GamePosition<*>.transform(transform: CoordinateTransform): GamePosition<*> {
    val turns = turns.map { turn ->
        turn.copy(moves = turn.moves.map {
            Move(transform.transform(it.coordinate), it.owner)
        })
    }

    return GamePosition(turns, nextTurn)
}

fun Board.transform(transform: CoordinateTransform): Board {
    val cells = cells.mapKeys { (coordinate, _) -> transform.transform(coordinate) }
    val lineHighlights = lineHighlights.map { highlight ->
        highlight.copy(
            start = transform.transform(highlight.start),
            direction = Direction.entries.first { it.direction == transform.transform(highlight.direction.direction) },
        )
    }

    return Board(
        cells = cells,
        lineHighlights = lineHighlights,
        attributes = attributes,
    )
}
