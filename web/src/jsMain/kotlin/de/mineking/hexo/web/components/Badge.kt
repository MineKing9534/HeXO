package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.HTMLSpanElement

@Composable
fun Badge(
    color: Color = Color.Neutral,
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
    content: ContentBuilder<HTMLSpanElement>,
) {
    Span({
        classes("inline-flex", "items-center", "gap-1.5", "rounded-full", "border", "px-2.5", "py-1", "text-xs", "font-medium")
        colorClasses(color)
        attrs?.invoke(this)
    }, content)
}
