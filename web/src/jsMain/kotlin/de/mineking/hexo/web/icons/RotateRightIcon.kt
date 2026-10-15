package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun RotateRightIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 24 24", {
        attr("aria-hidden", "true")
        attr("fill", "none")
        attr("stroke", "currentColor")
        attr("stroke-width", "2")
        attr("stroke-linecap", "round")
        attr("stroke-linejoin", "round")
        attrs?.invoke(this)
    }) {
        Path("M21 12a9 9 0 1 1-9-9 9.75 9.75 0 0 1 6.74 2.74L21 8")
        Path("M21 3v5h-5")
    }
}
