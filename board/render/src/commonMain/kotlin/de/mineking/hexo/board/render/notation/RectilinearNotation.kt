package de.mineking.hexo.board.render.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Cell
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.board.isEmpty
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.core.CellOwner
import kotlin.math.min

enum class RectilinearNotationType(
    val columnSeparator: String,
    val rowSeparator: String,
) {
    Compact("", "/") {
        private val regex = "\\.{2,}".toRegex()

        override fun String.postprocess() = regex.replace(this) {
            if (it.value.length == 2) "-" else it.value.length.toString()
        }
    },
    Multiline(" ", "\n"),
    ;

    open fun String.postprocess() = this
}

object RectilinearNotationBoardRenderer : BoardRenderer<RectilinearNotationType, String> {
    override suspend fun render(board: Board, param: RectilinearNotationType) = board.renderRectilinearNotation(param)

    fun withType(type: RectilinearNotationType) = object : BoardRenderer<Unit, String> {
        override suspend fun render(board: Board, param: Unit) = this@RectilinearNotationBoardRenderer.render(board, type)
    }
}

fun Board.renderRectilinearNotation(type: RectilinearNotationType) = renderRectilinearNotationInternal(type).notation

internal data class RenderedRectilinearNotation(val notation: String, val topLeft: CellCoordinate)

internal fun Board.renderRectilinearNotationInternal(type: RectilinearNotationType): RenderedRectilinearNotation {
    val lineHighlights = this@renderRectilinearNotationInternal.lineHighlights.groupBy { it.start }
    val renderCells = (lineHighlights.keys.associateWith { Cell() } + cells)
        .filter { (coordinate, cell) -> coordinate in lineHighlights || !cell.isEmpty(includeHighlights = true) }

    if (renderCells.isEmpty()) return RenderedRectilinearNotation("", CellCoordinate.Zero)

    val rows = renderCells.entries
        .groupBy { (coordinate, _) -> coordinate.r }
        .mapValues { (_, cells) -> cells.associate { (coordinate, cell) -> coordinate.q to cell } }

    val minQ = renderCells.keys.minOf { it.q }
    val minR = rows.keys.min()
    val maxR = rows.keys.max()

    val notation = buildString {
        for (r in minR..maxR) {
            rows[r]?.let { row ->
                append(type.columnSeparator.repeat(r - minR))
                appendRow(r, row, minQ, lineHighlights, type)
            }

            if (r < maxR) {
                append(type.rowSeparator)
            }
        }
    }.let { type.run { it.postprocess() } }

    return RenderedRectilinearNotation(notation, CellCoordinate(minQ, minR))
}

private fun StringBuilder.appendRow(
    r: Int,
    row: Map<Int, Cell>,
    minQ: Int,
    lineHighlights: Map<CellCoordinate, List<LineHighlight>>,
    type: RectilinearNotationType,
) {
    val maxQ = row.keys.max()

    for (q in min(minQ, row.keys.min())..maxQ) {
        val coordinate = CellCoordinate(q, r)
        appendCell(row[q])
        lineHighlights[coordinate]?.forEach { append(it.render()) }

        if (q < maxQ) {
            append(type.columnSeparator)
        }
    }
}

private fun LineHighlight.render(): String {
    val owner = when (color) {
        CellOwner.X -> "x"
        CellOwner.O -> "o"
        null -> ""
    }

    return "(${direction.symbol}$length$owner)"
}

private fun StringBuilder.appendCell(cell: Cell?) {
    append(cell?.owner?.symbol ?: ".")

    if (!cell?.label.isNullOrBlank()) {
        append("[${cell.label}]")
    }

    if (cell?.highlight != null) {
        append("(${cell.highlight?.color?.symbol ?: "!"})")
    }
}
