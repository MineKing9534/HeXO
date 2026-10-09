package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.MutableCell
import de.mineking.hexo.board.parse.LinkParser
import de.mineking.hexo.board.requireHexo
import kotlin.experimental.and
import kotlin.io.encoding.Base64

private const val SHIFT_BYTE = 0x80.toByte()
private const val MASK = 0x7f.toByte()

object TytoLinkParser : LinkParser(prefix = "https://hexo.tyto.cc/analysis#c=") {
    override suspend fun parseLink(param: String) = param.parseTytoNotation()
}

fun String.parseTytoNotation(): Board {
    val stream = TytoNotationStream(this)
    val board = MutableBoard()

    fun move(coordinate: CellCoordinate, turn: Int) {
        requireHexo(coordinate !in board.cells) { "Duplicate cell at $coordinate" }
        board[coordinate] = MutableCell(CellOwner.entries[turn % 2], turn = turn)
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
