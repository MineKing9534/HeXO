package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun ResetViewIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 24 24", {
        attr("aria-hidden", "true")
        attr("fill", "none")
        attr("stroke", "currentColor")
        attr("stroke-width", "2")
        attr("stroke-linecap", "round")
        attr("stroke-linejoin", "round")
        attrs?.invoke(this)
    }) {
        Path("M3 11.5 12 4l9 7.5")
        Path("M5 10.5V20h14v-9.5")
        Path("M9.5 20v-5h5v5")
    }
}
