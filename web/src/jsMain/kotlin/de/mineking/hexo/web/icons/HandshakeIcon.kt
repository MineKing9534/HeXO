package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun HandshakeIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 24 24", {
        attr("aria-hidden", "true")
        attr("fill", "none")
        attr("stroke", "currentColor")
        attr("stroke-width", "1.75")
        attr("stroke-linecap", "round")
        attr("stroke-linejoin", "round")
        attrs?.invoke(this)
    }) {
        Path("m2 10 4-6 4 1")
        Path("m22 10-4-6-5 1-5 5 1.5 1.5a2 2 0 0 0 2.8 0L14 10l5 5")
        Path("m2 10 4 7 5 4a2 2 0 0 0 2-1 2 2 0 0 0 3-1 2 2 0 0 0 3-2l3-7")
        Path("m9 16 4 4")
        Path("m12 14 4 5")
        Path("m15 12 4 5")
    }
}
