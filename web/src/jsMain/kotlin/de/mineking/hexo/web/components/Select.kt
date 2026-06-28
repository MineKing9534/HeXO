package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
fun <T> Select(entries: List<T>, current: T, onChange: (T) -> Unit) {
    Div({ classes("flex", "gap-3") }) {
        entries.forEach { entry ->
            Button({
                classes(
                    "rounded-full", "px-8", "py-1.25", "m-px", "text-xs", "font-medium", "ring-1", "backdrop-blur-sm", "transition-all",
                    "duration-200", "hover:shadow-lg", "cursor-pointer",
                )

                if (entry == current) {
                    classes(
                        "bg-amber-400/12", "ring-amber-200/40", "text-amber-300", "shadow-amber-950/30",
                        "hover:bg-amber-400/18", "hover:ring-amber-200/60", "hover:text-amber-200",
                    )
                } else {
                    classes(
                        "bg-slate-800/40", "ring-slate-600/60", "text-slate-300",
                        "hover:bg-slate-700/50", "hover:ring-slate-500/60", "hover:text-slate-100",
                    )
                }

                onClick { onChange(entry) }
            }) {
                Text(entry.toString())
            }
        }
    }
}
