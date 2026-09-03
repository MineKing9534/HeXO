package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import de.mineking.hexo.web.icons.ArrowLeftIcon
import de.mineking.hexo.web.layout.AppRoute
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun BackLink(route: AppRoute, text: String, compact: Boolean = false) {
    Anchor(route, {
        classes(
            "group", "inline-flex", "items-center", "gap-2", "rounded-lg", "border", "text-sm",
            "font-semibold", "transition", "focus:outline-none", "focus-visible:ring-2",
            "focus-visible:ring-slate-500/60",
        )
        if (compact) {
            classes(
                "border-slate-700", "bg-slate-900/70", "px-3", "py-1.5", "text-slate-300",
                "hover:border-slate-600", "hover:bg-slate-800", "hover:text-slate-100",
            )
        } else {
            classes(
                "border-slate-600", "bg-slate-800/70", "px-4", "py-2", "text-slate-200",
                "hover:border-slate-500", "hover:bg-slate-700/80", "hover:text-white",
            )
        }
    }) {
        ArrowLeftIcon {
            classes("size-4", "shrink-0", "transition-transform", "group-hover:-translate-x-0.5")
        }
        Span({ classes("whitespace-nowrap") }) {
            Text(text)
        }
    }
}
