package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.KeyboardEvent

@Composable
fun Tooltip(
    text: String,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    tooltipAttrs: AttrBuilderContext<HTMLSpanElement>? = null,
    showArrow: Boolean = true,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    Div({
        classes("relative")
        onMouseEnter { visible = true }
        onMouseLeave { visible = false }
        attrs?.invoke(this)
    }) {
        content()
        Span({
            classes(
                "absolute", "z-50", "rounded-lg", "border", "border-white/10", "bg-slate-800",
                "bottom-full", "left-1/2", "-translate-x-1/2", "mb-2", "w-max", "max-w-64",
                "px-3", "py-1.5", "text-xs", "font-medium", "leading-relaxed", "text-slate-200",
                "text-center", "whitespace-normal", "wrap-break-word", "shadow-md", "shadow-black/40",
            )

            if (visible) {
                classes("pointer-events-auto", "opacity-100", "transition-opacity", "duration-150", "ease-out")
            } else {
                classes("pointer-events-none", "opacity-0")
            }

            attr("role", "tooltip")
            tooltipAttrs?.invoke(this)
            ref { element ->
                val dispose = registerTooltipEvents(element) { visible = it }
                onDispose { dispose() }
            }
        }) {
            if (showArrow) {
                Span({
                    classes(
                        "absolute", "-bottom-1", "left-1/2", "size-2", "-translate-x-1/2", "rotate-45",
                        "border-r", "border-b", "border-white/10", "bg-slate-800",
                    )
                    attr("aria-hidden", "true")
                })
            }
            Span({ classes("relative", "block") }) {
                Text(text)
            }
        }
    }
}

private fun keepTooltipVisible(element: HTMLSpanElement) {
    var left = 0.0
    var right = window.innerWidth.toDouble()
    var ancestor = element.parentElement
    while (ancestor != null) {
        if (window.getComputedStyle(ancestor).overflowX in listOf("hidden", "clip", "auto", "scroll")) {
            val bounds = ancestor.getBoundingClientRect()
            left = maxOf(left, bounds.left)
            right = minOf(right, bounds.right)
        }
        ancestor = ancestor.parentElement
    }

    // Measure the requested placement before shifting it inside the available space.
    element.style.removeProperty("margin-left")
    element.style.removeProperty("max-width")
    val maxWidth = window.getComputedStyle(element).maxWidth
    val availableWidth = (right - left - 16).coerceAtLeast(0.0)
    element.style.maxWidth = if (maxWidth == "none") "${availableWidth}px" else "min($maxWidth, ${availableWidth}px)"
    val bounds = element.getBoundingClientRect()
    val offset = (left + 8 - bounds.left).coerceAtLeast(0.0) - (bounds.right - right + 8).coerceAtLeast(0.0)
    if (offset != 0.0) element.style.marginLeft = "${offset}px"
}

private fun registerTooltipEvents(element: HTMLSpanElement, onVisibilityChange: (Boolean) -> Unit): () -> Unit {
    val parent = element.parentElement!!
    val reposition = EventListener { keepTooltipVisible(element) }
    val focus = EventListener {
        val focused = parent.querySelector(":focus-visible") != null
        onVisibilityChange(focused)
        if (focused) keepTooltipVisible(element)
    }
    val hide = EventListener { onVisibilityChange(false) }
    val keyDown = EventListener {
        if ((it as KeyboardEvent).key == "Escape") onVisibilityChange(false)
    }

    parent.addEventListener("mouseenter", reposition)
    parent.addEventListener("focusin", focus)
    parent.addEventListener("focusout", hide)
    parent.addEventListener("pointerdown", hide)
    window.addEventListener("keydown", keyDown)
    window.addEventListener("blur", hide)
    window.addEventListener("resize", reposition)
    window.addEventListener("scroll", reposition, true)

    return {
        parent.removeEventListener("mouseenter", reposition)
        parent.removeEventListener("focusin", focus)
        parent.removeEventListener("focusout", hide)
        parent.removeEventListener("pointerdown", hide)
        window.removeEventListener("keydown", keyDown)
        window.removeEventListener("blur", hide)
        window.removeEventListener("resize", reposition)
        window.removeEventListener("scroll", reposition, true)
    }
}
