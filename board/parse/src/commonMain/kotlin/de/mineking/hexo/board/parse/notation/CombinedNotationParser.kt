package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.finalState
import de.mineking.hexo.board.mutable
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.requireHexo
import de.mineking.hexo.board.to
import de.mineking.hexo.board.translate
import de.mineking.hexo.board.withAttributes

class CombinedNotationParser(
    private val stateParser: NotationParser,
    private val turnParser: NotationParser,
    private val originLabel: String = "*",
    private val separator: String = "---",
) : NotationParser {
    companion object {
        val Default = CombinedNotationParser(RectilinearNotationParser, BKENotationParser)
    }

    @OptIn(InternalBoardApi::class)
    override suspend fun parse(notation: String): Board {
        val parts = notation.split(separator)
        requireHexo(parts.size == 2 && parts.all { it.isNotBlank() }) {
            "Combined notation must contain two non-empty parts separated by `$separator`"
        }

        val stateHistory = stateParser.parse(parts[0].trim())
        requireHexo(stateHistory.numberOfStates > 0) { "The initial state is empty" }

        val state = stateHistory.finalState.mutable().apply {
            cells.forEach {
                it.value.turn = null
            }
        }

        val origins = state.cells.filterValues {
            val isOrigin = it.label == originLabel
            if (isOrigin) it.label = ""

            isOrigin
        }.keys

        requireHexo(origins.size == 1) {
            "The initial state must contain exactly one cell labeled `$originLabel`, but found ${origins.size}"
        }

        val turns = turnParser.parse(parts[1].trim()).translate(origins.single())
        turns.cells.forEach { (coordinate, cell) ->
            requireHexo(cell.turn != null) { "Turn move at $coordinate missing turn" }
            requireHexo(state.cells[coordinate]?.owner == null) {
                "Turn move at $coordinate overlaps the initial state"
            }
        }

        return (state + turns).withAttributes(BoardAttribute.ShowTurnNumbers to true)
    }
}
