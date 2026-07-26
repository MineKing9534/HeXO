package de.mineking.hexo.board.latex

import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.LatexCommand
import de.mineking.kotlinlatex.ExpandLatex
import de.mineking.kotlinlatex.latex
import de.mineking.kotlinlatex.raw
import de.mineking.hexo.board.parse.parseRectilinearNotation
import de.mineking.hexo.board.render.image.tikz.renderToTikZ

@LatexCommand("hexo")
fun hexoCommand(
    padding: Latex = latex("32"),
    rawLabels: Latex = latex("true"),
    width: Latex = raw("\\linewidth"),
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
        width = width.source.takeUnless { it == "none" },
    )
    return raw(tikz)
}
