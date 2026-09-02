package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import de.mineking.hexo.web.icons.ArrowLeftIcon
import de.mineking.hexo.web.layout.AppRoute
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLAnchorElement

@Composable
fun BackLink(route: AppRoute, text: String, attrs: AttrBuilderContext<HTMLAnchorElement>? = null) {
    Anchor(route, {
        classes(
            "group", "inline-flex", "items-center", "gap-2", "rounded-lg", "border", "px-4", "py-2", "text-sm", "font-semibold", "shadow-sm",
            "border-emerald-500/40", "bg-emerald-500/10", "text-emerald-100", "shadow-emerald-950/20", "transition",
            "hover:border-emerald-400/60", "hover:bg-emerald-500/20", "hover:text-white",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60", "mt-2",
        )
        attrs?.invoke(this)
    }) {
        ArrowLeftIcon {
            classes("size-4", "shrink-0", "transition-transform", "group-hover:-translate-x-0.5")
        }
        Span({ classes("whitespace-nowrap") }) {
            Text(text)
        }
    }
}
