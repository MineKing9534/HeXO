package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import de.mineking.hexo.web.icons.AlertTriangleIcon
import de.mineking.hexo.web.layout.AppRoute
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun NotFoundCard(
    title: String,
    description: String,
    eyebrow: String = "Not found",
    action: (@Composable () -> Unit)? = null,
) {
    StatusCard({
        classes(
            "border-slate-700/80!", "bg-slate-900/70!", "shadow-xl", "shadow-black/25", "lg:max-w-2xl",
        )
    }) {
        Div({
            classes(
                "grid", "size-12", "place-items-center", "rounded-xl", "border", "border-rose-400/25",
                "bg-rose-500/10", "text-rose-300", "shadow-lg", "shadow-rose-950/20",
            )
        }) {
            AlertTriangleIcon { classes("size-6") }
        }
        Div({ classes("grid", "place-items-center", "gap-1.5", "text-center") }) {
            Span({ classes("text-xs", "font-bold", "tracking-widest", "text-rose-300", "uppercase") }) {
                Text(eyebrow)
            }
            H1({ classes("text-2xl", "font-extrabold", "text-slate-100", "sm:text-3xl") }) {
                Text(title)
            }
        }
        P({ classes("max-w-lg", "text-center", "text-sm", "leading-relaxed", "text-slate-400") }) {
            Text(description)
        }
        action?.let {
            Div({ classes("pt-1") }) { it() }
        }
    }
}

@Composable
fun NotFoundBackLink(route: AppRoute, text: String) {
    BackLink(route, text) {
        classes(
            "mt-0!", "border-slate-600!", "bg-slate-800/70!", "text-slate-200!", "shadow-none!",
            "hover:border-slate-500!", "hover:bg-slate-700/80!", "hover:text-white!",
            "focus-visible:ring-slate-500/60!",
        )
    }
}
