package de.mineking.hexo.board.latex

import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.LatexCommand
import de.mineking.kotlinlatex.latex
import de.mineking.kotlinlatex.raw
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.HDSTheme
import de.mineking.hexo.board.render.image.theme.HTTTXTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.render.image.theme.TytoTheme

@LatexCommand("hdstheme")
fun hdsTheme(
    gap: Latex = latex(""),
    borderThickness: Latex = latex(""),
    backgroundColor: Latex = latex(""),
    cellBorderColor: Latex = latex(""),
    highlightColor: Latex = latex(""),
    focusColor: Latex = latex(""),
    emptyCellBackgroundColor: Latex = latex(""),
    emptyCellLabelColor: Latex = latex(""),
    playerXColor: Latex = latex(""),
    playerOColor: Latex = latex(""),
) = themeSpec(
    "HDS",
    listOf(
        gap,
        borderThickness,
        backgroundColor,
        cellBorderColor,
        highlightColor,
        focusColor,
        emptyCellBackgroundColor,
        emptyCellLabelColor,
        playerXColor,
        playerOColor,
    ),
)

@LatexCommand("htttxtheme")
fun htttxTheme(
    gap: Latex = latex(""),
    borderThickness: Latex = latex(""),
    lineThickness: Latex = latex(""),
    backgroundColor: Latex = latex(""),
    cellBorderColor: Latex = latex(""),
    occupiedHighlightColor: Latex = latex(""),
    emptyHighlightColor: Latex = latex(""),
    lineHighlightColor: Latex = latex(""),
    playerXBackgroundColor: Latex = latex(""),
    playerXDecorationColor: Latex = latex(""),
    playerOBackgroundColor: Latex = latex(""),
    playerODecorationColor: Latex = latex(""),
) = themeSpec(
    "HTTTX",
    listOf(
        gap,
        borderThickness,
        lineThickness,
        backgroundColor,
        cellBorderColor,
        occupiedHighlightColor,
        emptyHighlightColor,
        lineHighlightColor,
        playerXBackgroundColor,
        playerXDecorationColor,
        playerOBackgroundColor,
        playerODecorationColor,
    ),
)

@LatexCommand("tytotheme")
fun tytoTheme(
    gap: Latex = latex(""),
    borderThickness: Latex = latex(""),
    backgroundColor: Latex = latex(""),
    emptyCellBackgroundColor: Latex = latex(""),
    emptyCellBorderColor: Latex = latex(""),
    occupiedCellBorderColor: Latex = latex(""),
    playerXColor: Latex = latex(""),
    playerOColor: Latex = latex(""),
) = themeSpec(
    "Tyto",
    listOf(
        gap,
        borderThickness,
        backgroundColor,
        emptyCellBackgroundColor,
        emptyCellBorderColor,
        occupiedCellBorderColor,
        playerXColor,
        playerOColor,
    ),
)

internal fun parseTheme(specification: String): Theme {
    val values = specification.split('|')
    return when (values[0]) {
        "HDS" -> HDSTheme.Default.let { default ->
            require(values.size == HDS_SPEC_SIZE) { "Invalid HDS theme specification" }
            HDSTheme(
                gap = values[1].orDefault(default.gap),
                borderThickness = values[2].orDefault(default.borderThickness),
                backgroundColor = values[3].orDefault(default.backgroundColor),
                cellBorderColor = values[4].orDefault(default.cellBorderColor),
                highlightColor = values[5].orDefault(default.highlightColor),
                focusColor = values[6].orDefault(default.focusColor),
                emptyCellBackgroundColor = values[7].orDefault(default.emptyCellBackgroundColor),
                emptyCellLabelColor = values[8].orDefault(default.emptyCellLabelColor),
                playerXColor = values[9].orDefault(default.playerXColor),
                playerOColor = values[10].orDefault(default.playerOColor),
            )
        }

        "HTTTX" -> HTTTXTheme.Default.let { default ->
            require(values.size == HTTTX_SPEC_SIZE) { "Invalid HTTTX theme specification" }
            HTTTXTheme(
                gap = values[1].orDefault(default.gap),
                borderThickness = values[2].orDefault(default.borderThickness),
                lineThickness = values[3].orDefault(default.lineThickness),
                backgroundColor = values[4].orDefault(default.backgroundColor),
                cellBorderColor = values[5].orDefault(default.cellBorderColor),
                occupiedHighlightColor = values[6].orDefault(default.occupiedHighlightColor),
                emptyHighlightColor = values[7].orDefault(default.emptyHighlightColor),
                lineHighlightColor = values[8].orDefault(default.lineHighlightColor),
                playerXBackgroundColor = values[9].orDefault(default.playerXBackgroundColor),
                playerXDecorationColor = values[10].orDefault(default.playerXDecorationColor),
                playerOBackgroundColor = values[11].orDefault(default.playerOBackgroundColor),
                playerODecorationColor = values[12].orDefault(default.playerODecorationColor),
            )
        }

        "Tyto" -> TytoTheme.Default.let { default ->
            require(values.size == TYTO_SPEC_SIZE) { "Invalid Tyto theme specification" }
            TytoTheme(
                gap = values[1].orDefault(default.gap),
                borderThickness = values[2].orDefault(default.borderThickness),
                backgroundColor = values[3].orDefault(default.backgroundColor),
                emptyCellBackgroundColor = values[4].orDefault(default.emptyCellBackgroundColor),
                emptyCellBorderColor = values[5].orDefault(default.emptyCellBorderColor),
                occupiedCellBorderColor = values[6].orDefault(default.occupiedCellBorderColor),
                playerXColor = values[7].orDefault(default.playerXColor),
                playerOColor = values[8].orDefault(default.playerOColor),
            )
        }

        else -> throw IllegalArgumentException(
            "Invalid theme specification; use \\hdstheme, \\htttxtheme, or \\tytotheme",
        )
    }
}

private fun themeSpec(name: String, values: List<Latex>) = raw(values.joinToString(prefix = "$name|", separator = "|") { it.source })
private fun String.orDefault(default: Double) = if (isEmpty()) default else toDouble()
private fun String.orDefault(default: Color) = if (isEmpty()) default else Color.parse(this)

private const val HDS_SPEC_SIZE = 11
private const val HTTTX_SPEC_SIZE = 13
private const val TYTO_SPEC_SIZE = 9
