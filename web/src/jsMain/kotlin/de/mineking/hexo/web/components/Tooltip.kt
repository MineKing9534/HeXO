package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget
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
        attrs?.invoke(this)
    }) {
        content()
        Span({
            classes(
                "fixed", "z-50", "rounded-lg", "border", "border-white/10", "bg-slate-800",
                "w-max", "max-w-64",
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
                val trigger = element.parentElement as HTMLDivElement
                document.body!!.appendChild(element)
                val dispose = registerTooltipEvents(element, trigger) { visible = it }
                onDispose {
                    dispose()
                    trigger.appendChild(element)
                }
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

private fun keepTooltipVisible(element: HTMLSpanElement, trigger: HTMLDivElement) {
    val triggerBounds = trigger.getBoundingClientRect()
    val viewportPadding = 8.0

    element.style.left = "0"
    element.style.top = "0"
    element.style.maxWidth = "min(16rem, calc(100vw - 16px))"

    val tooltipBounds = element.getBoundingClientRect()
    val centeredLeft = triggerBounds.left + (triggerBounds.width - tooltipBounds.width) / 2
    val left = centeredLeft.coerceIn(viewportPadding, window.innerWidth - tooltipBounds.width - viewportPadding)
    val top = (triggerBounds.top - tooltipBounds.height - viewportPadding).coerceAtLeast(viewportPadding)
    element.style.left = "${left}px"
    element.style.top = "${top}px"

    val arrow = element.querySelector("[aria-hidden='true']") as? HTMLSpanElement
    arrow?.style?.left = "${(triggerBounds.left + triggerBounds.width / 2 - left).coerceIn(8.0, tooltipBounds.width - 8)}px"
}

private fun registerTooltipEvents(
    element: HTMLSpanElement,
    trigger: HTMLDivElement,
    onVisibilityChange: (Boolean) -> Unit,
): () -> Unit {
    val reposition = EventListener { keepTooltipVisible(element, trigger) }
    val show = EventListener {
        keepTooltipVisible(element, trigger)
        onVisibilityChange(true)
    }
    val focus = EventListener {
        if (trigger.querySelector(":focus-visible") != null) show.handleEvent(it)
    }
    val hide = EventListener { onVisibilityChange(false) }
    val keyDown = EventListener {
        if ((it as KeyboardEvent).key == "Escape") onVisibilityChange(false)
    }

    val listeners = mutableListOf<() -> Unit>()
    fun EventTarget.listen(type: String, listener: EventListener, capture: Boolean = false) {
        addEventListener(type, listener, capture)
        listeners += { removeEventListener(type, listener, capture) }
    }

    trigger.listen("mouseenter", show)
    trigger.listen("mouseleave", hide)
    trigger.listen("focusin", focus)
    trigger.listen("focusout", hide)
    trigger.listen("pointerdown", hide)
    window.listen("keydown", keyDown)
    window.listen("blur", hide)
    window.listen("resize", reposition)
    window.listen("scroll", reposition, true)

    return { listeners.forEach { it() } }
}
