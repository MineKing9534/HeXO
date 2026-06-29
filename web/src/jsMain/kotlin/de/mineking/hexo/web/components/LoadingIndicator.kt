package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun LoadingIndicator(attrs: AttrBuilderContext<HTMLDivElement>? = null) {
    Div({
        classes("animate-spin", "rounded-full", "border-5", "border-slate-400/30", "border-t-emerald-400")
        attrs?.invoke(this)
    })
}
