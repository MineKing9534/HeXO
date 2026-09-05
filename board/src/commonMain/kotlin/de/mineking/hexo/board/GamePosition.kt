package de.mineking.hexo.board

const val DEFAULT_MOVES_PER_TURN = 2

interface Move {
    val coordinate: CellCoordinate
    val owner: CellOwner
}

@Suppress("FunctionNaming")
fun Move(coordinate: CellCoordinate, owner: CellOwner) = object : Move {
    override val coordinate = coordinate
    override val owner = owner
}

data class TurnMetaData(
    val player: CellOwner,
    val placementsRemaining: Int,
    val turn: Int,
)

data class Turn<out M : Move>(val meta: TurnMetaData, val moves: List<M>)
fun Turn<*>.isComplete() = meta.placementsRemaining == 0

data class GamePosition<out M : Move>(
    val turns: List<Turn<M>>,
    val nextTurn: TurnMetaData,
)

val <M : Move> GamePosition<M>.moves get() = turns.flatMap { it.moves }
fun <M : Move> GamePosition<M>.take(maxMoves: Int) = moves.take(maxMoves).toGamePosition()

fun GamePosition<*>.toBoard(
    focusWinningRows: Boolean = true,
    attributes: BoardAttributes = BoardAttributes(),
): Board = MutableBoard(attributes = attributes.copy()).apply {
    moves.forEachIndexed { index, move ->
        val cell = this[move.coordinate]
        cell.owner = move.owner

        cell.turn = (index + 1) / 2
    }

    if (focusWinningRows) {
        focusWinningRows()
    }
}

data class BoardToPositionResult(val state: Board, val turns: GamePosition<*>)

fun Board.findNextTurn(movesPerTurn: Int = DEFAULT_MOVES_PER_TURN) = toGamePosition(movesPerTurn).turns.nextTurn
fun Board.toGamePosition(movesPerTurn: Int = DEFAULT_MOVES_PER_TURN): BoardToPositionResult {
    val state = MutableBoard(attributes = attributes.copy(), lineHighlights = lineHighlights.toMutableList())
    val turns = mutableListOf<Turn<Move>>()

    cells.entries
        .groupBy { it.value.turn }
        .entries
        .sortedBy { it.key }
        .forEach { (turn, cells) ->
            if (turn == null) {
                cells.forEach { (coordinate, cell) ->
                    state[coordinate] = cell.copy()
                }
                return@forEach
            }

            val expected = turns
                .lastOrNull()
                ?.meta?.player?.other
                ?: requireNotNull(cells.first().value.owner)

            require(cells.all { it.value.owner == expected })

            turns += Turn(
                meta = TurnMetaData(
                    player = expected,
                    placementsRemaining = ((if (turn == 0) 1 else movesPerTurn) - cells.size).coerceIn(0, movesPerTurn),
                    turn = turn,
                ),
                moves = cells.map { Move(it.key, expected) },
            )
        }

    return BoardToPositionResult(state, GamePosition(
        turns = turns,
        nextTurn = turns.findNextTurn(
            hasState = !state.isEmpty(includeHighlights = false),
            movesPerTurn = movesPerTurn,
        ),
    ))
}

private fun List<Turn<*>>.findNextTurn(hasState: Boolean, movesPerTurn: Int): TurnMetaData {
    val lastTurn = lastOrNull()
    if (lastTurn != null && !lastTurn.isComplete()) return lastTurn.meta

    return TurnMetaData(
        player = lastTurn?.meta?.player?.other ?: CellOwner.X,
        turn = lastTurn?.meta?.turn?.let { it + 1 } ?: if (hasState) 1 else 0,
        placementsRemaining = if (lastTurn == null) 1 else movesPerTurn,
    )
}

fun <M : Move> List<M>.toGamePosition(movesPerTurn: Int = DEFAULT_MOVES_PER_TURN): GamePosition<M> {
    val turnData = fold(mutableListOf<Pair<CellOwner, MutableList<M>>>()) { turns, move ->
        val lastTurn = turns.lastOrNull()

        if (lastTurn == null || lastTurn.first != move.owner) {
            turns += move.owner to mutableListOf(move)
        } else {
            lastTurn.second += move
        }

        turns
    }

    if (turnData.isNotEmpty()) {
        val first = turnData.first().second
        require(first.size == 1)
        require(first.first().coordinate == CellCoordinate.Zero)
    }

    val turns = turnData.mapIndexed { index, (player, cells) ->
        Turn(
            meta = TurnMetaData(
                player = player,
                turn = index,
                placementsRemaining = ((if (index == 0) 1 else movesPerTurn) - cells.size).coerceIn(0, movesPerTurn),
            ),
            moves = cells,
        )
    }

    return GamePosition(
        turns = turns,
        nextTurn = turns.findNextTurn(hasState = false, movesPerTurn = movesPerTurn),
    )
}
