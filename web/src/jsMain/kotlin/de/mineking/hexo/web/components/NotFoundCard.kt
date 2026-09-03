package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import de.mineking.hexo.web.icons.AlertTriangleIcon
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun NotFoundCard(
    title: String,
    description: String,
    action: (@Composable () -> Unit)? = null,
) {
    StatusCard(compact = true) {
        Div({ classes("grid", "w-full", "gap-5") }) {
            CardHeader(
                title = title,
                supportingText = "The requested content could not be found",
                truncateSupportingText = false,
                iconAttrs = { classes("border-rose-400/25", "bg-rose-500/10", "text-rose-300") },
            ) {
                AlertTriangleIcon { classes("size-4") }
            }
            SubCard({ classes("p-4", "sm:p-5") }, SubCardVariant.Inset) {
                Span({ classes("text-xs", "font-bold", "tracking-wider", "text-slate-400", "uppercase") }) {
                    Text("What happened?")
                }
                P({ classes("mt-2", "max-w-2xl", "text-sm", "leading-relaxed", "text-slate-300") }) {
                    Text(description)
                }
                P({ classes("mt-2", "text-xs", "leading-relaxed", "text-slate-500") }) {
                    Text("Check the address or return to the previous overview and try another entry.")
                }
            }
            action?.let {
                Div({ classes("flex", "justify-end") }) {
                    it()
                }
            }
        }
    }
}
