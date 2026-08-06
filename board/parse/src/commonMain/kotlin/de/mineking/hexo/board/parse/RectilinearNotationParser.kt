package de.mineking.hexo.board.parse

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
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

object RectilinearNotationParser : BoardParser {
    override suspend fun parse(notation: String) = notation.parseRectilinearNotation(focusWinningRows = false)
}

private const val COLUMN_NOTATION_PREFIX = "c"

fun String.parseRectilinearNotation(focusWinningRows: Boolean = true): Board {
    val board = MutableBoard()

    val columnNotation = startsWith(COLUMN_NOTATION_PREFIX)
    val cursor = Cursor(board, columnNotation)

    var state = ParserState.Normal
    val buffer = StringBuilder()

    val contentOffset = if (columnNotation) COLUMN_NOTATION_PREFIX.length else 0
    substring(contentOffset).forEachIndexed { offset, ch ->
        state = state.handleChar(ch, offset + contentOffset, cursor, buffer)
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

private class Cursor(private val board: MutableBoard, columnNotation: Boolean) {
    private val stepDirection = if (columnNotation) Direction.BottomRight.direction else Direction.Right.direction
    private val newlineDirection = if (columnNotation) Direction.Right.direction else Direction.BottomRight.direction

    var position = CellCoordinate.Zero
        private set
    private var lineStart = position

    val previousPosition get() = position - stepDirection

    fun configureCurrent(block: MutableCell.() -> Unit) {
        board[position].block()
    }

    fun configurePrevious(block: MutableCell.() -> Unit) {
        requireHexo(position != lineStart) { "This operations requires a cell in the current row!" }
        board[previousPosition].block()
    }

    fun highlightLine(origin: CellCoordinate, direction: Direction, length: Int, color: CellOwner?) {
        board.highlightLine(origin, direction, length, color)
    }

    fun step(n: Int = 1) {
        position += stepDirection * n
    }

    fun newRow() {
        lineStart += newlineDirection
        position = lineStart
    }
}
