package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun GitHubIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 16 16", {
        attr("aria-hidden", "true")
        attr("fill", "currentColor")
        attrs?.invoke(this)
    }) {
        Path(
            "M8 1.2a6.8 6.8 0 0 0-2.15 13.25c.34.06.46-.15.46-.33v-1.2c-1.9.42-2.3-.81-2.3-.81-.31-.8-.76-1.01-.76-1.01-.62-.43.05-.42.05-.42.69.05 1.05.72 1.05.72.61 1.07 1.61.76 2 .58.06-.45.24-.76.44-.93-1.52-.18-3.12-.78-3.12-3.45 0-.76.27-1.38.7-1.87-.07-.18-.31-.9.07-1.84 0 0 .58-.19 1.88.72A6.37 6.37 0 0 1 8 4.38c.58 0 1.15.08 1.69.24 1.3-.91 1.87-.72 1.87-.72.38.94.14 1.66.07 1.84.44.49.7 1.11.7 1.87 0 2.68-1.6 3.27-3.13 3.44.25.22.47.65.47 1.32v1.95c0 .18.12.39.47.32A6.8 6.8 0 0 0 8 1.2Z",
        )
    }
}
