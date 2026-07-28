package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

@Composable
fun ScrollableView(
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    content: ContentBuilder<HTMLDivElement>,
) {
    Div({
        classes(
            "min-h-0", "overflow-y-auto", "scrollbar-thin", "[scrollbar-color:rgb(71_85_105/0.8)_transparent]",
            "[&::-webkit-scrollbar]:w-2", "[&::-webkit-scrollbar-track]:bg-transparent", "[&::-webkit-scrollbar-thumb]:rounded-full",
            "[&::-webkit-scrollbar-thumb]:bg-slate-600/70", "hover:[&::-webkit-scrollbar-thumb]:bg-slate-500/80",
        )
        attrs?.invoke(this)
    }, content)
}
