package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.web.components.theme
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.rgba

@Composable
fun <T> rememberPrevious(key: Any?, value: T): T? {
    var previous by remember(key) { mutableStateOf<T?>(null) }

    LaunchedEffect(value) {
        previous = value
    }

    return previous
}

val CellOwner.cssColor: CSSColorValue get() {
    val color = theme.run { color(default = Color.Transparent) }
    return rgba(color.red.toInt(), color.green.toInt(), color.blue.toInt(), color.alpha.toInt())
}
