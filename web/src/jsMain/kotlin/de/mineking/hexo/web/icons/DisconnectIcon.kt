package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun DisconnectIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 24 24", {
        attr("aria-hidden", "true")
        attr("fill", "currentColor")
        attrs?.invoke(this)
    }) {
        Path("M2 2h13a1 1 0 0 1 1 1v6h-1.5V3.5h-12v15h12V15H16v4a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1Z")
        Path("M2 3a.75.75 0 0 1 1.1-.66l5.5 3A.75.75 0 0 1 9 6v16a.75.75 0 0 1-1.1.66l-6.5-3.55A.75.75 0 0 1 1 18.45V3Z")
        Path("M14 10.5h4V8.75a.6.6 0 0 1 .96-.48l4.3 3.25a.6.6 0 0 1 0 .96l-4.3 3.25a.6.6 0 0 1-.96-.48V13.5h-4a1.5 1.5 0 0 1 0-3Z")
    }
}
