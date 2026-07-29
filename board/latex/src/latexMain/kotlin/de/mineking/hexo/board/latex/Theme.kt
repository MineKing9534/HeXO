package de.mineking.hexo.board.latex

import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.raw
import de.mineking.hexo.board.render.image.theme.Color

internal fun themeSpec(name: String, values: List<Latex>) = raw(values.joinToString(prefix = "$name|", separator = "|") { it.source })

internal fun String.orDefault(default: Double) = if (isEmpty()) default else toDouble()
internal fun String.orDefault(default: Color) = if (isEmpty()) default else Color.parse(this)
