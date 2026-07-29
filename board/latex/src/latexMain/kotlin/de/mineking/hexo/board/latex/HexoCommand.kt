package de.mineking.hexo.board.latex

import de.mineking.hexo.board.parse.parseRectilinearNotation
import de.mineking.hexo.board.render.image.DEFAULT_VISIBLE_RADIUS
import de.mineking.hexo.board.render.image.tikz.renderToTikZ
import de.mineking.kotlinlatex.ExpandLatex
import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.LatexCommand
import de.mineking.kotlinlatex.raw

@LatexCommand("hexo")
fun hexoCommand(
    padding: Latex = raw("32"),
    rawLabels: Latex = raw("true"),
    focusWinningRows: Latex = raw("true"),
    visibleRadius: Latex = raw("$DEFAULT_VISIBLE_RADIUS"),
    width: Latex = raw("none"),
    scale: Latex = raw("none"),
    fading: Latex = raw("0"),
    @ExpandLatex theme: Latex = raw("\\hdstheme"),
    @ExpandLatex notation: Latex,
): Latex {
    val board = notation.source.parseRectilinearNotation(focusWinningRows = focusWinningRows.toBoolean("focusWinningRows"))
    val tikz = board.renderToTikZ(
        padding = padding.source.toInt(),
        visibleRadius = visibleRadius.source.toInt(),
        theme = theme.parseTheme(),
        rawLabels = rawLabels.toBoolean("rawLabels"),
        labelStyle = "\\hexolabelfont",
    )

    return raw(buildString {
        if (scale.source != "none") append("\\scalebox{${scale.source}}{")
        if (width.source != "none") append("\\resizebox{${width.source}}{!}{")

        if (fading.source != "0") append("\\fadeedges[${fading.source}]{")
        append(tikz)
        if (fading.source != "0") append("}")

        if (width.source != "none") append("}")
        if (scale.source != "none") append("}")
    })
}
