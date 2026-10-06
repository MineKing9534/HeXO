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

interface AbstractTurnMetaData {
    val player: CellOwner
    val turn: Int
}

data class TurnMetaData(
    override val player: CellOwner,
    override val turn: Int,
) : AbstractTurnMetaData

data class NextTurnMetaData(
    override val player: CellOwner,
    override val turn: Int,
    val placementsRemaining: Int,
) : AbstractTurnMetaData

fun AbstractTurnMetaData.withRemaining(remaining: Int) = NextTurnMetaData(player, turn, remaining)

data class Turn<out M : Move>(val meta: TurnMetaData, val moves: List<M>)

data class GamePosition<out M : Move>(
    val turns: List<Turn<M>>,
    val nextTurn: NextTurnMetaData,
)

val <M : Move> GamePosition<M>.moves get() = turns.flatMap { it.moves }
fun <M : Move> GamePosition<M>.take(maxMoves: Int): GamePosition<M> {
    require(maxMoves >= 0) { "Requested move count $maxMoves is less than zero." }
    if (maxMoves >= moves.size) return this

    val selectedTurns = mutableListOf<Turn<M>>()
    var remainingMoves = maxMoves

    for (turn in turns) {
        if (remainingMoves == 0) {
            return GamePosition(selectedTurns, turn.meta.withRemaining(remainingAfter(turn, 0)))
        }

        if (remainingMoves < turn.moves.size) {
            selectedTurns += turn.copy(
                moves = turn.moves.take(remainingMoves),
            )
            return GamePosition(selectedTurns, turn.meta.withRemaining(remainingAfter(turn, remainingMoves)))
        }

        selectedTurns += turn
        remainingMoves -= turn.moves.size
    }

    return this
}

private fun GamePosition<*>.remainingAfter(turn: Turn<*>, selectedMoves: Int): Int {
    val originalRemaining = nextTurn.placementsRemaining
        .takeIf { nextTurn.player == turn.meta.player && nextTurn.turn == turn.meta.turn }
        ?: 0

    return turn.moves.size - selectedMoves + originalRemaining
}

fun GamePosition<*>.toBoard(
    focusWinningRows: Boolean = true,
    attributes: BoardAttributes = BoardAttributes(),
): Board = MutableBoard(attributes = attributes.copy()).apply {
    turns.forEach { turn ->
        turn.moves.forEach { move ->
            val cell = this[move.coordinate]
            cell.owner = turn.meta.player
            cell.turn = turn.meta.turn
        }
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

fun List<Turn<*>>.findNextTurn(hasState: Boolean, movesPerTurn: Int = DEFAULT_MOVES_PER_TURN): NextTurnMetaData {
    val lastTurn = lastOrNull()
    if (lastTurn != null) {
        val placements = if (lastTurn.meta.turn == 0) 1 else movesPerTurn
        val remaining = (placements - lastTurn.moves.size).coerceAtLeast(0)
        if (remaining > 0) return lastTurn.meta.withRemaining(remaining)
    }

    return NextTurnMetaData(
        player = lastTurn?.meta?.player?.other ?: CellOwner.X,
        turn = lastTurn?.meta?.turn?.let { it + 1 } ?: if (hasState) 1 else 0,
        placementsRemaining = if (lastTurn == null) 1 else movesPerTurn,
    )
}

fun <M : Move> List<M>.toTurns(): List<Turn<M>> {
    val turnData = fold(mutableListOf<Pair<CellOwner, MutableList<M>>>()) { turns, move ->
        val lastTurn = turns.lastOrNull()

        if (lastTurn == null || lastTurn.first != move.owner) {
            turns += move.owner to mutableListOf(move)
        } else {
            lastTurn.second += move
        }

        turns
    }

    return turnData.mapIndexed { index, (player, cells) ->
        Turn(
            meta = TurnMetaData(
                player = player,
                turn = index,
            ),
            moves = cells,
        )
    }
}

fun <M : Move> List<M>.toGamePosition(movesPerTurn: Int = DEFAULT_MOVES_PER_TURN): GamePosition<M> {
    val turns = toTurns()
    return GamePosition(
        turns = turns,
        nextTurn = turns.findNextTurn(hasState = false, movesPerTurn = movesPerTurn),
    )
}
