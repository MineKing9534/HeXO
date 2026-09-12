package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    action: (@Composable () -> Unit)? = null,
) {
    SubCard(variant = SubCardVariant.Empty) {
        Div({ classes("flex", "flex-col", "items-center", "gap-2") }) {
            H2({ classes("text-base", "font-semibold", "text-slate-200") }) { Text(title) }
            P({ classes("max-w-md", "text-sm", "leading-relaxed", "text-slate-500") }) {
                Text(description)
            }
            action?.invoke()
        }
    }
}
