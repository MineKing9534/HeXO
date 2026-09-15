@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.HTMLSpanElement

enum class BadgeSize {
    Small,
    Large,
}

@Composable
fun Badge(
    color: Color = Color.Neutral,
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
    size: BadgeSize = BadgeSize.Small,
    content: ContentBuilder<HTMLSpanElement>,
) {
    Span({
        classes("inline-flex", "items-center", "rounded-full", "border", "font-medium")
        when (size) {
            BadgeSize.Small -> classes("gap-1.5", "px-2.5", "py-1", "text-xs")
            BadgeSize.Large -> classes("gap-2", "px-3", "py-1.5", "text-sm")
        }
        colorClasses(color)
        attrs?.invoke(this)
    }, content)
}
