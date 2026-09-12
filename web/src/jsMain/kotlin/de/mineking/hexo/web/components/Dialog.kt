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
    supportingText: String? = null,
    actionRow: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Div({
        onClick { onClose() }
        classes("fixed", "inset-0", "z-50", "grid", "place-items-center", "bg-slate-950/70", "p-3")
    }) {
        Div({
            onClick { it.stopPropagation() }
            classes(
                "relative", "max-h-[calc(100dvh-1.5rem)]", "w-full", "max-w-xl", "overflow-y-auto",
                "rounded-xl", "border", "border-slate-700", "bg-slate-900", "p-4", "pt-3",
                "shadow-2xl", "sm:p-5", "sm:pt-3",
            )
        }) {
            Div({ classes("space-y-6") }) {
                if (title != null) {
                    Div({ classes("flex", "items-start", "justify-between", "gap-4", "pr-8") }) {
                        Div({ classes("min-w-0") }) {
                            H1({ classes("text-xl", "font-bold", "text-slate-100") }) {
                                Text(title)
                            }
                            if (supportingText != null) {
                                Div({ classes("mt-1", "text-sm", "leading-snug", "text-slate-500") }) {
                                    Text(supportingText)
                                }
                            }
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
