package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import de.mineking.hexo.web.icons.CloseIcon
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text

@Composable
fun Dialog(
    title: String?,
    onClose: () -> Unit,
    actionRow: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Div({
        onClick { onClose() }
        classes("fixed", "inset-0", "z-50", "grid", "place-items-center", "bg-slate-950/70")
    }) {
        Div({
            onClick { it.stopPropagation() }
            classes("relative", "w-full", "max-w-xl", "rounded-xl", "border", "border-slate-700", "bg-slate-900", "p-5", "pt-3", "shadow-2xl")
        }) {
            Div({ classes("space-y-6") }) {
                if (title != null) {
                    Div({ classes("flex", "items-center", "justify-between", "gap-4", "pr-8") }) {
                        H1({ classes("text-lg", "font-bold", "text-slate-100") }) {
                            Text(title)
                        }
                    }
                }

                Button({
                    attr("aria-label", "Close")
                    classes(
                        "absolute", "right-1", "top-1", "grid", "size-8", "place-items-center", "rounded-md",
                        "text-slate-400", "transition", "hover:text-rose-400",
                    )
                    onClick { onClose() }
                }) {
                    CloseIcon()
                }

                Div({ classes("flex", "flex-col", "space-y-2") }) {
                    content()
                }

                if (actionRow != null) {
                    Div({ classes("flex", "justify-end") }) {
                        actionRow()
                    }
                }
            }
        }
    }
}
