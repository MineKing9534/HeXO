package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLSpanElement

@Composable
fun EloBadge(
    elo: Int,
    adjustment: Int? = null,
    attrs: AttrBuilderContext<HTMLSpanElement>? = null,
) {
    Span({
        classes(
            "inline-flex", "max-w-full", "flex-wrap", "items-center", "justify-center", "gap-2",
            "px-2.5", "py-1",
            "tabular-nums",
        )
        attr("aria-label", "Elo rating $elo" + when {
            adjustment == null -> ""
            adjustment > 0 -> ", gained $adjustment points"
            adjustment < 0 -> ", lost ${-adjustment} points"
            else -> ", unchanged"
        })
        attrs?.invoke(this)
    }) {
        Span({ classes("text-[10px]", "font-semibold", "tracking-wider", "text-slate-500") }) { Text("ELO") }
        Span({ classes("text-sm", "font-bold", "text-slate-100") }) { Text("$elo") }
        if (adjustment != null) {
            Span({
                classes("rounded-md", "px-1.5", "py-0.5", "text-xs", "font-semibold", adjustment.color)
                classes(when {
                    adjustment > 0 -> "bg-emerald-400/10"
                    adjustment < 0 -> "bg-rose-400/10"
                    else -> "bg-slate-400/10"
                })
            }) {
                Text(if (adjustment > 0) "+$adjustment" else "$adjustment")
            }
        }
    }
}

private val Int.color get() = when {
    this > 0 -> "text-emerald-300"
    this < 0 -> "text-rose-300"
    else -> "text-slate-300"
}
