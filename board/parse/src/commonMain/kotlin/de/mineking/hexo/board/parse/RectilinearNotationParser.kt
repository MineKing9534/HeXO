package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.MutableCell
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.minus
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.requireHexo
import de.mineking.hexo.board.times
import de.mineking.hexo.core.CellOwner

object RectilinearNotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseRectilinearNotation(focusWinningRows = false)
}

fun String.parseRectilinearNotation(focusWinningRows: Boolean = true): Board {
    val board = MutableBoard()
    val cursor = Cursor(board)

    var state = ParserState.Normal
    val buffer = StringBuilder()

    forEachIndexed { offset, ch ->
        state = state.handleChar(ch, offset, cursor, buffer)
    }

    state.handleEOF(cursor, buffer)

    requireHexo(buffer.isEmpty()) { "Unterminated symbol at end of input: `$buffer`" }

    if (focusWinningRows) {
        board.focusWinningRows()
    }

    requireHexo(!board.isEmpty(includeHighlights = true)) { "Cannot parse an empty board" }

    return board
}

private enum class ParserState {
    Normal {
        override fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState {
            return when {
                ch.isDigit() -> GapDigits.handleChar(ch, offset, cursor, buffer)
                ch == '(' -> Highlight
                ch == '[' -> Label
                else -> {
                    cursor.handleNormalChar(ch, offset)
                    this
                }
            }
        }

        private fun Cursor.handleNormalChar(ch: Char, offset: Int) {
            when (ch) {
                ' ' -> return
                '/', '\n' -> {
                    newRow()
                    return
                }

                'x', 'X' -> configureCurrent { owner = CellOwner.X }
                'o', 'O' -> configureCurrent { owner = CellOwner.O }
                '.', '!' -> {}
                '-' -> step()
                else -> throw HexoNotationException("Unexpected character `$ch` at offset $offset")
            }

            if (ch.isUpperCase() || ch == '!') {
                configureCurrent {
                    highlight = CellHighlight(null)
                }
            }

            step()
        }
    },
    GapDigits {
        override fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState {
            if (ch.isDigit()) {
                buffer.append(ch)
                return this
            } else {
                handleEOF(cursor, buffer)
                return Normal.handleChar(ch, offset, cursor, buffer)
            }
        }

        override fun handleEOF(cursor: Cursor, buffer: StringBuilder) {
            cursor.step(buffer.toString().toInt())
            buffer.clear()
        }
    },
    Label {
        override fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState {
            if (ch == ']') {
                cursor.configurePrevious { label = buffer.toString() }
                buffer.clear()

                return Normal
            } else {
                buffer.append(ch)
                return this
            }
        }
    },
    Highlight {
        private val pattern = """^\s*(?:([bdpq<>])\s*(\d*)\s*)?([xo!]?)\s*$""".toRegex()

        override fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState {
            if (ch == ')') {
                cursor.highlight(buffer.toString())
                buffer.clear()

                return Normal
            } else {
                buffer.append(ch)
                return this
            }
        }

        private fun Cursor.highlight(notation: String) {
            val match = pattern.matchEntire(notation)
            requireHexo(match != null) { "Invalid highlight notation, use `([b,d,p,q,<,>]<length>?[x,o]?)` for lines or `([x,o]?)` for cells" }

            val color = when (match.groupValues[3]) {
                "x" -> CellOwner.X
                "o" -> CellOwner.O
                else -> null
            }

            if (match.groupValues[1].isNotBlank()) {
                val direction = Direction.fromSymbol(match.groupValues[1])
                val length = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 6
                highlightLine(previousPosition, direction, length, color)
            } else {
                configurePrevious {
                    requireHexo(highlight == null) { "Cannot overwrite cell highlight" }
                    highlight = CellHighlight(color)
                }
            }
        }
    },
    ;

    abstract fun handleChar(ch: Char, offset: Int, cursor: Cursor, buffer: StringBuilder): ParserState
    open fun handleEOF(cursor: Cursor, buffer: StringBuilder) {}
}

private class Cursor(private val board: MutableBoard) {
    companion object {
        private val STEP_DIRECTION = CellCoordinate(1, 0)
        private val NEWLINE_DIRECTION = CellCoordinate(0, 1)
    }

    var position = CellCoordinate.Zero
        private set

    val previousPosition get() = position - STEP_DIRECTION

    fun configureCurrent(block: MutableCell.() -> Unit) {
        board[position].block()
    }

    fun configurePrevious(block: MutableCell.() -> Unit) {
        requireHexo(position.q >= 1) { "This operations requires a cell in the current row!" }
        board[position - STEP_DIRECTION].block()
    }

    fun highlightLine(origin: CellCoordinate, direction: Direction, length: Int, color: CellOwner?) {
        board.highlightLine(origin, direction, length, color)
    }

    fun step(n: Int = 1) {
        position += STEP_DIRECTION * n
    }

    fun newRow() {
        position += NEWLINE_DIRECTION
        position = position.copy(q = 0)
    }
}
