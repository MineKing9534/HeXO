package de.mineking.hexo.board.latex

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.tikz.renderToTikZ
import de.mineking.kotlinlatex.ExpandLatex
import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.LatexExport
import de.mineking.kotlinlatex.raw

private const val SEPARATOR = "\u001f"

@LatexExport
fun Board.renderTikZDiagram(
    cache: List<String>,
    padding: Latex = raw("32"),
    rawLabels: Latex = raw("true"),
    visibleRadius: Latex = raw("$DEFAULT_VISIBLE_RADIUS"),
    width: Latex = raw("none"),
    scale: Latex = raw("none"),
    fading: Latex = raw("0"),
    @ExpandLatex cacheVersion: Latex = raw("\\hexocacheversion"),
    @ExpandLatex theme: Latex = raw("\\hdstheme"),
): Latex {
    val tikz = renderToTikZ(
        padding = padding.source.toInt(),
        visibleRadius = visibleRadius.source.toInt(),
        theme = theme.parseTheme(),
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
            visibleRadius.source + SEPARATOR +
            width.source + SEPARATOR +
            scale.source + SEPARATOR +
            fading.source + SEPARATOR +
            theme.source + SEPARATOR +
            cache.joinToString(SEPARATOR),
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
