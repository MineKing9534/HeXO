package de.mineking.hexo.board.latex

import de.mineking.hexo.board.parse.parseRectilinearNotation
import de.mineking.hexo.board.render.image.tikz.renderToTikZ
import de.mineking.kotlinlatex.ExpandLatex
import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.LatexCommand
import de.mineking.kotlinlatex.raw

@LatexCommand("hexo")
fun hexoCommand(
    padding: Latex = raw("32"),
    rawLabels: Latex = raw("true"),
    width: Latex = raw("none"),
    scale: Latex = raw("none"),
    @ExpandLatex theme: Latex = raw("\\hdstheme"),
    @ExpandLatex notation: Latex,
): Latex {
    require(rawLabels.source == "true" || rawLabels.source == "false") {
        "rawLabels must be either `true` or `false`"
    }

    val board = notation.source.parseRectilinearNotation()
    val tikz = board.renderToTikZ(
        padding = padding.source.toInt(),
        theme = parseTheme(theme.source),
        rawLabels = rawLabels.source == "true",
        labelStyle = "\\hexolabelfont",
    )

    return raw(buildString {
        if (scale.source != "none") append("\\scalebox{${scale.source}}{")
        if (width.source != "none") append("\\resizebox{${width.source}}{!}{")

        append(tikz)

        if (width.source != "none") append("}")
        if (scale.source != "none") append("}")
    })
}
