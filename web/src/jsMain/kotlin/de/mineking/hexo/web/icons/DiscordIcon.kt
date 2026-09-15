package de.mineking.hexo.web.icons

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.w3c.dom.svg.SVGElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun DiscordIcon(attrs: AttrBuilderContext<SVGElement>? = null) {
    Svg("0 0 16 16", {
        attr("aria-hidden", "true")
        attr("fill", "currentColor")
        attrs?.invoke(this)
    }) {
        Path(
            "M13.545 2.907a13.2 13.2 0 0 0-3.257-1.011.05.05 0 0 0-.052.025c-.141.25-.297.577-.405.833a12.4 12.4 0 0 0-3.658 0 9 9 0 0 0-.412-.833.05.05 0 0 0-.052-.025c-1.125.194-2.246.54-3.257 1.011a.05.05 0 0 0-.021.018C.356 6.024-.213 9.047.066 12.033a.05.05 0 0 0 .02.034 13.3 13.3 0 0 0 3.995 2.02.05.05 0 0 0 .056-.019c.308-.42.582-.863.817-1.329a.05.05 0 0 0-.01-.059l-.017-.01a8.6 8.6 0 0 1-1.248-.595.05.05 0 0 1-.005-.084q.126-.094.248-.19a.05.05 0 0 1 .052-.007c2.619 1.196 5.454 1.196 8.041 0a.05.05 0 0 1 .053.006q.122.1.248.191a.05.05 0 0 1-.004.084 8 8 0 0 1-1.249.594.05.05 0 0 0-.03.03.05.05 0 0 0 .003.04c.24.465.513.908.817 1.329a.05.05 0 0 0 .056.019 13.2 13.2 0 0 0 4.001-2.02.05.05 0 0 0 .019-.033c.334-3.451-.56-6.449-2.366-9.109a.04.04 0 0 0-.02-.018M5.547 10.21c-.788 0-1.438-.724-1.438-1.612s.637-1.613 1.438-1.613c.807 0 1.45.731 1.438 1.613 0 .888-.637 1.612-1.438 1.612m4.907 0c-.788 0-1.438-.724-1.438-1.612s.637-1.613 1.438-1.613c.807 0 1.45.731 1.438 1.613 0 .888-.631 1.612-1.438 1.612",
        )
    }
}
