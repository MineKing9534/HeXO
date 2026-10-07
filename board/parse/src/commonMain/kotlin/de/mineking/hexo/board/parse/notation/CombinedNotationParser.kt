package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.HexoNotationFormatException
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
    private val separator: String = "||",
) : NotationParser {
    companion object {
        val Default = CombinedNotationParser(RectilinearNotationParser, BKENotationParser)
    }

    @OptIn(InternalBoardApi::class)
    override suspend fun parse(notation: String): Board {
        val hasSeparator = separator in notation
        var rawStateError: HexoNotationException? = null
        val rawState = try {
            stateParser.parse(notation)
        } catch (_: HexoNotationFormatException) {
            null
        } catch (e: HexoNotationException) {
            if (!hasSeparator) throw e
            rawStateError = e
            null
        }

        if (rawState != null) return rawState
        if (!hasSeparator) return turnParser.parse(notation)

        val parts = notation.split(separator)
        requireHexo(parts.size == 2 && parts.all { it.isNotBlank() }, notationCheck = true) {
            "Combined notation must contain two non-empty parts separated by `$separator`"
        }

        val stateHistory = try {
            stateParser.parse(parts[0].trim())
        } catch (e: HexoNotationException) {
            throw rawStateError ?: e
        }
        requireHexo(stateHistory.numberOfStates > 0) { "The initial state is empty" }

        val state = stateHistory.finalState.mutable()
        val origins = state.cells.filterValues {
            it.turn = null

            val isOrigin = it.label == originLabel
            if (isOrigin) it.label = ""

            isOrigin
        }.keys

        requireHexo(origins.size == 1, notationCheck = origins.isEmpty()) {
            "The initial state must contain exactly one cell labeled `$originLabel`, but found ${origins.size}"
        }

        val turns = parsePart(turnParser, parts[1].trim()).translate(origins.single())
        turns.cells.forEach { (coordinate, cell) ->
            requireHexo(cell.turn != null) { "Turn move at $coordinate missing turn" }
            requireHexo(state.cells[coordinate]?.owner == null) {
                "Turn move at $coordinate overlaps the initial state"
            }
        }

        return (state + turns).withAttributes(BoardAttribute.ShowTurnNumbers to true)
    }

    private suspend fun parsePart(parser: NotationParser, notation: String) = try {
        parser.parse(notation)
    } catch (e: HexoNotationFormatException) {
        // The separator identifies combined notation even if a component is unrecognizable.
        throw HexoNotationException(e.message, e)
    }
}
