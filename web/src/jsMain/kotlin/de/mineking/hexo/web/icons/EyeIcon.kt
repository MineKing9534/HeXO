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
fun EyeIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 24 24", {
        attr("aria-hidden", "true")
        attr("fill", "none")
        attr("stroke", "currentColor")
        attr("stroke-width", "2")
        attr("stroke-linecap", "round")
        attr("stroke-linejoin", "round")
        attrs?.invoke(this)
    }) {
        Path("M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z")
        Circle(attrs = {
            cx(12)
            cy(12)
            r(3)
        })
    }
}
