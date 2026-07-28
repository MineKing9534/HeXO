package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun StarIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 16 16", {
        attr("aria-hidden", "true")
        attr("fill", "currentColor")
        attrs?.invoke(this)
    }) {
        Path("M8 1.9l1.7 3.46 3.82.56-2.76 2.69.65 3.8L8 10.59 4.6 12.4l.65-3.8L2.5 5.92l3.8-.56L8 1.9Z")
    }
}
