package de.mineking.hexo.board.latex

import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.mutable
import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.kotlinlatex.ExpandLatex
import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.LatexCommand
import de.mineking.kotlinlatex.raw
import kotlinx.coroutines.runBlocking

@OptIn(InternalBoardApi::class)
@LatexCommand("hexo")
fun hexoCommand(
    padding: Latex = raw("32"),
    rawLabels: Latex = raw("true"),
    focusWinningRows: Latex = raw("true"),
    visibleRadius: Latex = raw("$DEFAULT_VISIBLE_RADIUS"),
    width: Latex = raw("none"),
    scale: Latex = raw("none"),
    fading: Latex = raw("0"),
    compact: Latex = raw("true"),
    @ExpandLatex cacheVersion: Latex = raw("\\hexocacheversion"),
    @ExpandLatex theme: Latex = raw("\\hdstheme"),
    @ExpandLatex notation: Latex,
): Latex {
    val board = runBlocking { // BoardParser.Default only has synchronous implementations so using the dummy runBlocking is safe
        BoardParser.Default.parse(notation.source).mutable().apply {
            if (focusWinningRows.source.toBooleanStrict()) focusWinningRows()
        }
    }

    return board.renderTikZDiagram(
        cache = listOf(focusWinningRows.source, notation.source),
        padding = padding,
        rawLabels = rawLabels,
        compact = compact,
        visibleRadius = visibleRadius,
        width = width,
        scale = scale,
        fading = fading,
        cacheVersion = cacheVersion,
        theme = theme,
    )
}
