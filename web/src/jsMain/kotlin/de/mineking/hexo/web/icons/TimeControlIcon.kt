package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.dom.svg.Circle
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun TimeControlIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 16 16", {
        attr("aria-hidden", "true")
        attr("fill", "none")
        attr("stroke", "currentColor")
        attrs?.invoke(this)
    }) {
        Circle(attrs = {
            cx(8)
            cy(8)
            r(5.25)
            attr("stroke-width", "1.5")
        })
        Path("M8 5.2v3.2l2.1 1.25", attrs = {
            attr("stroke-width", "1.5")
            attr("stroke-linecap", "round")
            attr("stroke-linejoin", "round")
        })
    }
}
