package de.mineking.hexo.solver

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.minus
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.times

private fun Board.transform(offset: CellCoordinate, flip: Boolean): Board = MutableBoard().apply {
    cells += this@transform.cells.mapNotNull { (coordinate, cell) ->
        val owner = cell.owner ?: return@mapNotNull null

        val cell = cell.copy()
        if (flip) cell.owner = owner.other

        (coordinate + offset) to cell
    }.toMap()
}

internal data class BoardTransformResult(
    val offset: CellCoordinate,
    val didFlip: Boolean,
    val board: Board,
) {
    constructor(board: Board) : this(CellCoordinate.Zero, false, board)

    fun flipPlayer(player: CellOwner) = if (didFlip) player.other else player

    fun transformBack(coordinate: CellCoordinate) = coordinate - offset

    fun transformBack(defense: Defense) = Defense(
        first = transformBack(defense.first),
        second = defense.second?.let { transformBack(it) },
    )

    private fun transformBack(turn: Turn) = Turn(
        player = if (didFlip) turn.player.other else turn.player,
        cells = turn.cells.map { transformBack(it) },
    )

    fun transformBack(result: FindWinResult.Win) = result.copy(
        turns = result.turns.map { transformBack(it) },
    )
}

// Ensure that (0, 0) is a cell with P1
internal fun Board.transform(): BoardTransformResult {
    if (cells[CellCoordinate.Zero]?.owner == CellOwner.X) return BoardTransformResult(this)

    val (coordinate, owner) = findAnchor() ?: return BoardTransformResult(this)

    val offset = coordinate * -1
    val flip = owner != CellOwner.X
    val transformed = transform(offset, flip)

    return BoardTransformResult(offset, flip, transformed)
}

private fun Board.findAnchor(): Pair<CellCoordinate, CellOwner>? {
    var result: Pair<CellCoordinate, CellOwner>? = null
    for ((coordinate, cell) in cells) {
        val owner = cell.owner
        if (owner == CellOwner.X) {
            result = coordinate to owner
            break
        }

        if (result == null && owner == CellOwner.O) {
            result = coordinate to owner
        }
    }

    return result
}
