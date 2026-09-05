package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.web.icons.ChevronDownIcon
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div

@Composable
fun DropdownMenu(
    label: @Composable () -> Unit,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    Div({ classes("relative", "min-w-0") }) {
        Button({
            classes(
                "flex", "w-full", "cursor-pointer", "items-center", "justify-between", "gap-2", "rounded-lg",
                "border", "border-slate-700", "bg-slate-900/90", "px-3", "py-2.5", "text-sm",
                "font-semibold", "text-slate-200", "shadow-lg", "shadow-black/20",
            )
            attr("aria-expanded", open.toString())
            onClick { open = !open }
        }) {
            Div({ classes("min-w-0", "truncate") }) { label() }
            ChevronDownIcon {
                classes("size-4", "shrink-0", "transition-transform")
                if (open) classes("rotate-180")
            }
        }
        if (open) {
            Div({
                classes(
                    "absolute", "inset-x-0", "top-[calc(100%+0.5rem)]", "z-40", "grid", "gap-1",
                    "rounded-xl", "border", "border-slate-700", "bg-slate-950/98", "p-1.5",
                    "shadow-2xl", "shadow-black/50", "backdrop-blur-md",
                )
            }) {
                content { open = false }
            }
        }
    }
}
