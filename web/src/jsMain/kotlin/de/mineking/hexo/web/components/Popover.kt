@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent

enum class PopoverPlacement { Top, Bottom }
enum class PopoverAlignment { Start, Center, End }

@Composable
fun Popover(
    label: String,
    placement: PopoverPlacement = PopoverPlacement.Bottom,
    alignment: PopoverAlignment = PopoverAlignment.Start,
    boundarySelector: String? = null,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    popoverAttrs: AttrBuilderContext<HTMLDivElement>? = null,
    trigger: @Composable (open: Boolean, toggle: () -> Unit) -> Unit,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val placement by rememberUpdatedState(placement)
    val alignment by rememberUpdatedState(alignment)
    val boundarySelector by rememberUpdatedState(boundarySelector)
    var reposition by remember { mutableStateOf<(() -> Unit)?>(null) }

    Div({
        classes("relative", "inline-flex", "min-w-0")
        attrs?.invoke(this)
    }) {
        trigger(open) { open = !open }
        if (open) {
            // Compose removes children by index, so the portalled panel needs a stable placeholder.
            Div({ classes("contents") }) {
                Div({
                    classes(
                        "fixed", "z-50", "overflow-y-auto", "rounded-2xl", "border", "border-slate-700/60",
                        "bg-slate-950/95", "p-3", "text-sm", "text-slate-200",
                        "shadow-lg", "shadow-black/25", "backdrop-blur-xl",
                    )
                    attr("role", "dialog")
                    attr("aria-label", label)
                    attr("tabindex", "-1")
                    popoverAttrs?.invoke(this)
                    ref { element ->
                        val placeholder = element.parentElement as HTMLDivElement
                        val anchor = placeholder.parentElement as HTMLDivElement
                        document.body!!.appendChild(element)
                        val position = { positionPopover(element, anchor, placement, alignment, boundarySelector) }
                        val disposePositioning = observePopoverPosition(element, anchor, boundarySelector, position)
                        val disposeDismissal = registerPopoverDismissal(element, anchor) { open = false }
                        reposition = position
                        position()
                        element.focus()
                        onDispose {
                            disposeDismissal()
                            disposePositioning()
                            reposition = null
                            placeholder.appendChild(element)
                        }
                    }
                }) { content { open = false } }
            }
        }
    }
    SideEffect { reposition?.invoke() }
}

private fun positionPopover(
    element: HTMLDivElement,
    trigger: HTMLDivElement,
    placement: PopoverPlacement,
    alignment: PopoverAlignment,
    boundarySelector: String?,
) {
    val boundary = boundarySelector?.let { trigger.closest(it)?.getBoundingClientRect() }
    val left = maxOf(12.0, boundary?.left?.plus(12) ?: 12.0)
    val right = minOf(window.innerWidth - 12.0, boundary?.right?.minus(12) ?: (window.innerWidth - 12.0))
    val top = maxOf(12.0, boundary?.top?.plus(12) ?: 12.0)
    val bottom = minOf(window.innerHeight - 12.0, boundary?.bottom?.minus(12) ?: (window.innerHeight - 12.0))
    element.style.maxWidth = "${(right - left).coerceAtLeast(0.0)}px"
    element.style.maxHeight = "${(bottom - top).coerceAtLeast(0.0)}px"

    val anchor = trigger.getBoundingClientRect()
    val height = element.getBoundingClientRect().height
    val above = (anchor.top - top - 8).coerceAtLeast(0.0)
    val below = (bottom - anchor.bottom - 8).coerceAtLeast(0.0)
    val placeAbove = when (placement) {
        PopoverPlacement.Top -> above >= height || above >= below
        PopoverPlacement.Bottom -> below < height && above > below
    }
    element.style.maxHeight = "${if (placeAbove) above else below}px"
    val bounds = element.getBoundingClientRect()
    val alignedLeft = when (alignment) {
        PopoverAlignment.Start -> anchor.left
        PopoverAlignment.Center -> anchor.left + (anchor.width - bounds.width) / 2
        PopoverAlignment.End -> anchor.right - bounds.width
    }
    val alignedTop = if (placeAbove) anchor.top - bounds.height - 8 else anchor.bottom + 8
    element.style.left = "${alignedLeft.coerceIn(left, (right - bounds.width).coerceAtLeast(left))}px"
    element.style.top = "${alignedTop.coerceIn(top, (bottom - bounds.height).coerceAtLeast(top))}px"
}

private fun observePopoverPosition(
    element: HTMLDivElement,
    trigger: HTMLDivElement,
    boundarySelector: String?,
    position: () -> Unit,
): () -> Unit {
    val observer = ResizeObserver(position)
    val reposition = EventListener { position() }
    observer.observe(element)
    observer.observe(trigger)
    boundarySelector?.let { trigger.closest(it) }?.let { observer.observe(it) }
    window.addEventListener("resize", reposition)
    window.addEventListener("scroll", reposition, true)
    return {
        observer.disconnect()
        window.removeEventListener("resize", reposition)
        window.removeEventListener("scroll", reposition, true)
    }
}

private const val FOCUSABLE_SELECTOR =
    "button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])"

private fun registerPopoverDismissal(
    element: HTMLDivElement,
    trigger: HTMLDivElement,
    onClose: () -> Unit,
): () -> Unit {
    val returnFocus = (document.activeElement as? HTMLElement)?.takeIf { trigger.contains(it) }
        ?: trigger.querySelector(FOCUSABLE_SELECTOR) as? HTMLElement

    var restoreFocus = true
    fun close(restore: Boolean) {
        restoreFocus = restore
        if (restore) returnFocus?.focus()
        onClose()
    }
    val outside = EventListener { event ->
        val target = event.target
        if (target !is Node || (!element.contains(target) && !trigger.contains(target))) close(false)
    }
    val escape = EventListener { event ->
        if (event is KeyboardEvent && event.key == "Escape" && !event.defaultPrevented) {
            event.preventDefault()
            close(true)
        }
    }
    val popupKeyDown = EventListener { event ->
        // Panel interactions must not activate shortcuts on the surrounding page.
        event.stopPropagation()
        escape.handleEvent(event)
    }
    val blur = EventListener { close(false) }
    val listeners = mutableListOf<() -> Unit>()
    fun EventTarget.listen(type: String, listener: EventListener, capture: Boolean = false) {
        addEventListener(type, listener, capture)
        listeners += { removeEventListener(type, listener, capture) }
    }
    document.listen("pointerdown", outside, true)
    document.listen("focusin", outside, true)
    document.listen("keydown", escape)
    element.listen("keydown", popupKeyDown)
    window.listen("blur", blur)
    return {
        listeners.forEach { it() }
        if (restoreFocus && element.contains(document.activeElement)) returnFocus?.focus()
    }
}

private external class ResizeObserver(callback: () -> Unit) {
    fun observe(element: Element)
    fun disconnect()
}
