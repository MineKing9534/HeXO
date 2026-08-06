package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.requireHexo
import kotlin.experimental.and
import kotlin.io.encoding.Base64

private const val SHIFT_BYTE = 0x80.toByte()
private const val MASK = 0x7f.toByte()

object TytoNotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseTytoNotation(focusWinningRows = false)
}

fun String.parseTytoNotation(focusWinningRows: Boolean = true): Board {
    val stream = TytoNotationStream(this)
    val board = MutableBoard()

    fun move(coordinate: CellCoordinate, turn: Int) {
        requireHexo(coordinate !in board.cells) { "Duplicate cell at $coordinate" }

        board[coordinate].apply {
            this.owner = CellOwner.entries[turn % 2]
            this.turn = turn
        }
    }

    move(CellCoordinate.Zero, turn = 0)

    var turn = 1
    while (stream.hasData()) {
        val first = stream.readCoordinate()
        val second = stream.readCoordinateOrNull()

        move(first, turn)
        if (second != null) move(second, turn)
        turn++
    }

    if (focusWinningRows) {
        board.focusWinningRows()
    }

    return board
}

private class TytoNotationStream(input: String) {
    companion object {
        private val base64 = Base64.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
    }

    private var offset = 0

    @Suppress("SwallowedException")
    private val bytes = try {
        base64.decode(input)
    } catch (e: IllegalArgumentException) {
        throw HexoNotationException(e.message ?: "Invalid base64")
    }

    fun hasData() = offset < bytes.size

    private fun readByte(): Byte {
        requireHexo(hasData()) { "Unexpected EOF on input" }
        return bytes[offset++]
    }

    private fun readInt(): Int {
        var e = 0
        var b: Byte
        while (readByte().also { b = it } == SHIFT_BYTE) e += 7

        return (b and MASK) * (1 shl e)
    }

    private fun Int.unzig() = if (this % 2 != 0) -(this + 1) / 2 else this / 2

    fun readCoordinate() = CellCoordinate(readInt().unzig(), readInt().unzig())
    fun readCoordinateOrNull() = if (hasData()) readCoordinate() else null
}
