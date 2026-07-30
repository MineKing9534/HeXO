package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun TournamentIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 16 16", {
        attr("aria-hidden", "true")
        attr("fill", "none")
        attr("stroke", "currentColor")
        attrs?.invoke(this)
    }) {
        Path("M5 2h6v2.5C11 7 9.7 8.25 8 8.25S5 7 5 4.5V2Z", attrs = {
            attr("stroke-width", "1.5")
            attr("stroke-linejoin", "round")
        })
        Path("M5 3.25H2.75v1C2.75 6 3.7 7 5.45 7M11 3.25h2.25v1C13.25 6 12.3 7 10.55 7M8 8.25V11m-2.5 2.5h5M6 11h4v2.5H6V11Z", attrs = {
            attr("stroke-width", "1.5")
            attr("stroke-linecap", "round")
            attr("stroke-linejoin", "round")
        })
    }
}
