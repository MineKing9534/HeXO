package de.mineking.hexo.board.latex

import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.mutable
import de.mineking.hexo.board.parse.BoardParser
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.tikz.renderToTikZ
import de.mineking.kotlinlatex.ExpandLatex
import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.LatexCommand
import de.mineking.kotlinlatex.raw
import kotlinx.coroutines.runBlocking

private const val SEPARATOR = "\u001f"

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
    @ExpandLatex cacheVersion: Latex = raw("\\hexocacheversion"),
    @ExpandLatex theme: Latex = raw("\\hdstheme"),
    @ExpandLatex notation: Latex,
): Latex {
    val board = runBlocking { // BoardParser.Default only has synchronous implementations so using the dummy runBlocking is safe
        BoardParser.Default.parse(notation.source).mutable().apply {
            if (focusWinningRows.source.toBooleanStrict()) focusWinningRows()
        }
    }

    val tikz = board.renderToTikZ(
        padding = padding.source.toInt(),
        visibleRadius = visibleRadius.source.toInt(),
        theme = theme.parseThemeCached(),
        rawLabels = rawLabels.source.toBooleanStrict(),
        labelStyle = "\\hexolabelfont",
    )

    val rendered = buildString {
        if (scale.source != "none") append("\\scalebox{${scale.source}}{")
        if (width.source != "none") append("\\resizebox{${width.source}}{!}{")

        if (fading.source != "0") append("\\fadeedges[${fading.source}]{")
        append(tikz)
        if (fading.source != "0") append("}")

        if (width.source != "none") append("}")
        if (scale.source != "none") append("}")
    }
    val cacheKey = stableCacheKey(
        cacheVersion.source + SEPARATOR +
            padding.source + SEPARATOR +
            rawLabels.source + SEPARATOR +
            focusWinningRows.source + SEPARATOR +
            visibleRadius.source + SEPARATOR +
            width.source + SEPARATOR +
            scale.source + SEPARATOR +
            fading.source + SEPARATOR +
            theme.source + SEPARATOR +
            notation.source
    )

    return raw("\\hexopreparepicture{$cacheKey}$rendered\\hexofinishpicture")
}

private fun stableCacheKey(value: String): String {
    var first = 0
    var second = 5381
    value.forEach { character ->
        first = 31 * first + character.code
        second = 33 * second + character.code
    }
    return "${first.toString().replace('-', 'n')}-${second.toString().replace('-', 'n')}"
}
