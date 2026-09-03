package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

enum class RatedFilter(val rated: Boolean?, val queryValue: String, val label: String) {
    All(null, "all", "All"),
    Rated(true, "rated", "Rated"),
    Casual(false, "casual", "Casual"),
    ;

    companion object {
        fun fromQuery(value: String?) = entries.find { it.queryValue == value } ?: All
    }
}

@Composable
fun RatedFilterControl(current: RatedFilter, onChange: (RatedFilter) -> Unit) {
    Div({
        attr("aria-label", "Filter by game type")
        attr("role", "group")
        classes(
            "inline-flex", "shrink-0", "items-center", "gap-0.5", "rounded-lg", "border",
            "border-slate-700/70", "bg-slate-950/45", "p-0.5",
        )
    }) {
        RatedFilter.entries.forEach { filter ->
            Button({
                attr("aria-pressed", (filter == current).toString())
                classes(
                    "cursor-pointer", "rounded-md", "border", "px-2", "py-1", "text-xs", "font-semibold",
                    "transition-colors", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-amber-300/50",
                    "sm:px-3",
                )
                if (filter == current) {
                    classes("shadow-sm", "shadow-black/20")
                    when (filter) {
                        RatedFilter.All -> classes("border-sky-400/30", "bg-sky-500/20", "text-sky-200")
                        RatedFilter.Rated -> classes("border-amber-400/30", "bg-amber-500/20", "text-amber-200")
                        RatedFilter.Casual -> classes("border-emerald-400/30", "bg-emerald-500/20", "text-emerald-200")
                    }
                } else {
                    classes("border-transparent", "text-slate-500")
                    when (filter) {
                        RatedFilter.All -> classes("hover:bg-sky-500/10", "hover:text-sky-300")
                        RatedFilter.Rated -> classes("hover:bg-amber-500/10", "hover:text-amber-300")
                        RatedFilter.Casual -> classes("hover:bg-emerald-500/10", "hover:text-emerald-300")
                    }
                }
                onClick { onChange(filter) }
            }) {
                Text(filter.label)
            }
        }
    }
}
