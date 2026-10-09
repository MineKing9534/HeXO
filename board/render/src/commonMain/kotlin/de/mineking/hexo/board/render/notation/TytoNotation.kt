package de.mineking.hexo.board.render.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.GamePosition
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.toGamePositionForce
import okio.Buffer
import kotlin.io.encoding.Base64

private const val SHIFT_BYTE = 0x80
private const val MASK = 0x7f

object TytoNotationBoardRenderer : BoardRenderer<Unit, String> {
    override suspend fun render(board: Board, param: Unit) = board
        .toGamePositionForce()
        .renderTytoNotation()
}

fun GamePosition<*>.renderTytoNotation(): String {
    val sink = Buffer()
    val stream = TytoNotationStream(sink)

    val first = turns.first()
    require(first.meta.player == CellOwner.X)
    require(first.moves.size == 1)
    require(first.moves.first().coordinate == CellCoordinate.Zero)

    val turns = turns.drop(1)

    turns.forEachIndexed { index, turn ->
        val min = if (index == turns.lastIndex) 1 else 2
        require(turn.moves.size in min..2)

        turn.moves.forEach {
            stream.writeCoordinate(it.coordinate)
        }
    }

    return Base64
        .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
        .encode(sink.readByteArray())
}

private class TytoNotationStream(private val out: Buffer) {
    private fun writeInt(value: Int) {
        var z = value
        while (z > MASK) {
            out.writeByte(z and MASK or SHIFT_BYTE)
            z /= 128
        }

        out.writeByte(z)
    }

    private fun Int.zig() = if (this >= 0) {
        this * 2
    } else {
        this * -2 - 1
    }

    fun writeCoordinate(coordinate: CellCoordinate) {
        writeInt(coordinate.q.zig())
        writeInt(coordinate.r.zig())
    }
}
