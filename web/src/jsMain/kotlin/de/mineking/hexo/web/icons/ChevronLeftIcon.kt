package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.jetbrains.compose.web.svg.fill
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun ChevronLeftIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 16 16", {
        fill("none")
        attr("stroke", "currentColor")
        attr("stroke-width", "1.8")
        attr("stroke-linecap", "round")
        attr("stroke-linejoin", "round")
        attr("aria-hidden", "true")
        attrs?.invoke(this)
    }) {
        Path("M10 3.5 5.5 8 10 12.5")
    }
}
