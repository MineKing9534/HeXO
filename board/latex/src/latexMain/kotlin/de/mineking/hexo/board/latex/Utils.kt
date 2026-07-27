package de.mineking.hexo.board.latex

import de.mineking.kotlinlatex.Latex

fun Latex.toBoolean(name: String): Boolean {
    require(source == "true" || source == "false") { "$name must be either `true` or `false`" }
    return source == "true"
}
