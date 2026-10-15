package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun MirrorHorizontalIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 24 24", {
        attr("aria-hidden", "true")
        attr("fill", "none")
        attr("stroke", "currentColor")
        attr("stroke-width", "1.75")
        attr("stroke-linecap", "round")
        attr("stroke-linejoin", "round")
        attrs?.invoke(this)
    }) {
        Path("M3 12h18", attrs = { attr("stroke-dasharray", "2 3") })
        Path("m7 3 10 5H7V3Z")
        Path("m7 21 10-5H7v5Z")
    }
}
