package de.mineking.hexo.hds.model

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttributes
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.copy
import de.mineking.hexo.board.focusWinningRows

interface Move {
    val coordinate: CellCoordinate
    val owner: CellOwner
}

interface AbstractGamePosition {
    val moves: List<Move>
}

fun AbstractGamePosition.asBoard(
    maxMoves: Int = moves.size,
    focusWinningRows: Boolean = true,
    attributes: BoardAttributes = BoardAttributes(),
): Board = MutableBoard(attributes = attributes.copy()).apply {
    val maxMoves = maxMoves.coerceIn(0, moves.size)
    repeat(maxMoves) {
        val move = moves[it]

        val cell = this[move.coordinate]
        cell.owner = move.owner

        cell.turn = (it + 1) / 2
    }

    if (focusWinningRows) {
        focusWinningRows()
    }
}
