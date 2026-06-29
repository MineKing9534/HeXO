package de.mineking.hexo.web

import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.web.components.theme
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.rgba

val CellOwner.cssColor: CSSColorValue get() {
    val color = theme.run { color(default = Color.Transparent) }
    return rgba(color.red.toInt(), color.green.toInt(), color.blue.toInt(), color.alpha.toInt())
}
