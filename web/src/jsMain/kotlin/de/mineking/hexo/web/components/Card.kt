package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Section
import org.w3c.dom.HTMLDivElement

@Composable
fun Card(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({
        classes(
            "rounded-2xl", "border", "border-slate-800", "bg-slate-800/40",
            "shadow-xl", "shadow-black/10",
        )
        attrs?.invoke(this)
    }, content)
}

@Composable
fun ContentCard(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({ classes("lg:h-12") })
    Card({
        classes("max-w-5xl", "w-full", "mx-auto")
        attrs?.invoke(this)
    }, content)
}

@Composable
fun StatusCard(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({ classes("mx-6", "flex-1", "h-full", "flex", "flex-col") }) {
        ContentCard({
            classes("grid", "place-items-center", "p-8", "min-h-48", "my-auto", "lg:max-w-3xl")
            attrs?.invoke(this)
        }) {
            Section({ classes("grid", "place-items-center", "gap-4", "w-full") }) {
                content()
            }
        }
    }
}

@Composable
fun SubCard(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({
        classes(
            "rounded-xl", "border", "border-slate-600/40", "bg-slate-700/40",
            "shadow-md", "shadow-slate-700/10",
        )
        attrs?.invoke(this)
    }, content)
}
