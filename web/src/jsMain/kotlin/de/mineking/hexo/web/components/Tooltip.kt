package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement

@Composable
fun Tooltip(
    text: String,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    tooltipAttrs: AttrBuilderContext<HTMLSpanElement>? = null,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var hideJob by remember { mutableStateOf<Job?>(null) }

    fun show() {
        hideJob?.cancel()
        visible = true
    }

    fun hideLater() {
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(200)
            visible = false
        }
    }

    Div({
        classes("group/tooltip", "relative")
        onMouseEnter { show() }
        onMouseLeave { hideLater() }
        attrs?.invoke(this)
    }) {
        content()
        Span({
            classes(
                "absolute", "z-50", "rounded-lg", "border", "border-white/20", "bg-slate-900/95",
                "px-3", "py-2", "text-xs", "font-medium", "leading-5", "text-slate-100",
                "shadow-2xl", "shadow-black/50", "backdrop-blur-md",
                "origin-right", "transition-all", "duration-150", "ease-out",
                "group-focus-within/tooltip:pointer-events-auto",
                "group-focus-within/tooltip:scale-100", "group-focus-within/tooltip:opacity-100",
            )
            if (visible) {
                classes("pointer-events-auto", "scale-100", "opacity-100")
            } else {
                classes("pointer-events-none", "scale-95", "opacity-0")
            }
            onMouseEnter { show() }
            onMouseLeave { hideLater() }
            attr("role", "tooltip")
            tooltipAttrs?.invoke(this)
        }) {
            Span({
                classes(
                    "absolute", "-right-1", "top-1/2", "size-2", "-translate-y-1/2", "rotate-45",
                    "border-r", "border-t", "border-white/20", "bg-slate-900",
                )
                attr("aria-hidden", "true")
            })
            Span({ classes("relative", "block") }) {
                Text(text)
            }
        }
    }
}
