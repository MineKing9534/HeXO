@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

enum class LoadingIndicatorSize {
    Tiny,
    Small,
    Medium,
    Large,
}

@Composable
fun LoadingIndicator(
    size: LoadingIndicatorSize = LoadingIndicatorSize.Large,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
) {
    Div({
        classes("animate-spin", "rounded-full", "border-slate-400/30", "border-t-emerald-400")
        when (size) {
            LoadingIndicatorSize.Tiny -> classes("size-4", "border-2")
            LoadingIndicatorSize.Small -> classes("size-5", "border-4")
            LoadingIndicatorSize.Medium -> classes("size-6", "border-5")
            LoadingIndicatorSize.Large -> classes("size-9", "border-5")
        }
        attrs?.invoke(this)
    })
}

@Composable
fun CardLoadingState(label: String) {
    Div({
        classes("grid", "min-h-64", "place-items-center")
        attr("aria-label", label)
        attr("role", "status")
    }) {
        LoadingIndicator()
    }
}
