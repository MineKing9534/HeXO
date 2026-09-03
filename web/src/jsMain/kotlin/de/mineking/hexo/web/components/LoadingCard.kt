package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
fun LoadingCard(label: String) {
    StatusCard({
        classes(
            "border-slate-700/80!", "bg-slate-900/70!", "shadow-xl", "shadow-black/25", "lg:max-w-2xl",
        )
    }) {
        LoadingIndicator { classes("size-9") }
        P({ classes("font-semibold", "text-slate-200") }) {
            Text(label)
        }
    }
}
