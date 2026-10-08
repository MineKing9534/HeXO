package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.MutableBoard
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.minus
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.parse.generated.RectilinearLexer
import de.mineking.hexo.board.parse.generated.RectilinearParser
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.requireHexo
import de.mineking.hexo.board.times

object RectilinearNotationParser : NotationParser {
    override suspend fun parse(notation: String) = notation.parseRectilinearNotation()
}

fun String.parseRectilinearNotation(): Board {
    val root = parseInternal(this)
    val builder = RectilinearBoardBuilder(columnNotation = root.column != null)

    root.item().forEach { item ->
        when {
            item.cell() != null -> builder.addCell(item.cell()!!.text)
            item.empty() != null -> builder.addCell(item.empty()!!.text)
            item.step() != null -> builder.step(2)
            item.gap() != null -> builder.addGap(item.gap()!!.text)
            item.row() != null -> builder.newRow()
            item.label() != null -> builder.addLabel(item.label()!!.text)
            item.highlight() != null -> builder.addHighlight(item.highlight()!!)
        }
    }

    val hasBoardStructure = root.column != null || root.item().any { it.gap() == null && it.text.isNotBlank() }
    requireHexo(!builder.board.isEmpty(includeHighlights = true), notationCheck = !hasBoardStructure) {
        "Cannot parse an empty board"
    }
    return builder.board
}

private fun parseInternal(input: String) = parseANTLRNotation(input, ::RectilinearLexer, ::RectilinearParser, RectilinearParser::root)

private fun RectilinearParser.HighlightColorContext.toOwner() = when (text) {
    "x" -> CellOwner.X
    "o" -> CellOwner.O
    "!" -> null
    else -> error("ANTLR accepted an unknown highlight color `$text`")
}

private class RectilinearBoardBuilder(columnNotation: Boolean) {
    val board = MutableBoard()
    private val stepDirection = if (columnNotation) Direction.BottomRight.direction else Direction.Right.direction
    private val newlineDirection = if (columnNotation) Direction.Right.direction else Direction.BottomRight.direction

    private var lineStart = CellCoordinate.Zero
    private var position = CellCoordinate.Zero
    private val previousPosition get() = position - stepDirection

    fun addCell(symbol: String) {
        val owner = when (symbol.lowercase()) {
            "x" -> CellOwner.X
            "o" -> CellOwner.O
            ".", "!" -> null
            else -> error("ANTLR accepted an unknown rectilinear cell `$symbol`")
        }
        val highlighted = symbol.first().isUpperCase() || symbol == "!"

        if (owner != null || highlighted) {
            board[position].apply {
                this.owner = owner
                if (highlighted) highlight = CellHighlight(null)
            }
        }
        step()
    }

    fun addGap(text: String) {
        step(text.toIntOrNull() ?: throw HexoNotationException("Rectilinear gap `$text` is too large"))
    }

    fun addLabel(text: String) {
        previousCell().label = buildString {
            var index = 1
            while (index < text.lastIndex) {
                if (text[index] == '\\') index++
                append(text[index])
                index++
            }
        }
    }

    fun addHighlight(highlight: RectilinearParser.HighlightContext) {
        val line = highlight.lineHighlight()
        if (line == null) {
            previousCell().apply {
                requireHexo(this.highlight == null) { "Cannot overwrite cell highlight" }
                this.highlight = CellHighlight(highlight.cellHighlight()!!.highlightColor()?.toOwner())
            }
            return
        }

        val lengthText = line.INTEGER()?.text
        val length = lengthText?.toIntOrNull()
            ?: if (lengthText == null) 6 else throw HexoNotationException("Line highlight length `$lengthText` is too large")

        board.highlightLine(
            previousPosition,
            Direction.fromSymbol(line.DIRECTION().text),
            length,
            line.highlightColor()?.toOwner(),
        )
    }

    fun step(count: Int = 1) {
        position += stepDirection * count
    }

    fun newRow() {
        lineStart += newlineDirection
        position = lineStart
    }

    private fun previousCell() = run {
        requireHexo(position != lineStart) { "This operations requires a cell in the current row!" }
        board[previousPosition]
    }
}
