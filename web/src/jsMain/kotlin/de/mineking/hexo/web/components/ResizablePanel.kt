package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticMouseEvent
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.MouseEvent

@Composable
fun ResizableTrailingPanel(
    defaultWidth: Int,
    minWidth: Int,
    maxWidth: Int,
    attrs: AttrBuilderContext<HTMLDivElement>,
    content: ContentBuilder<HTMLDivElement>,
) {
    var width by remember { mutableStateOf(defaultWidth) }
    var resizing by remember { mutableStateOf(false) }

    ResizeEffect(
        resizing = resizing,
        minWidth = minWidth,
        maxWidth = maxWidth,
        onResize = { width = it },
        onResizeEnd = { resizing = false },
    )

    Div({
        attrs()
        attr("style", "--sidebar-width: ${width}px")
    }) {
        ResizeHandle(resizing) { event ->
            event.preventDefault()
            resizing = true
        }
        content(this)
    }
}

@Composable
private fun ResizeEffect(
    resizing: Boolean,
    minWidth: Int,
    maxWidth: Int,
    onResize: (Int) -> Unit,
    onResizeEnd: () -> Unit,
) {
    DisposableEffect(resizing) {
        if (!resizing) return@DisposableEffect onDispose {}

        val mouseMove = EventListener { event ->
            event.preventDefault()
            val mouseEvent = event as MouseEvent
            onResize((window.innerWidth - mouseEvent.clientX).coerceIn(minWidth, maxWidth))
        }
        val mouseUp = EventListener {
            onResizeEnd()
        }

        window.addEventListener("mousemove", mouseMove)
        window.addEventListener("mouseup", mouseUp)
        onDispose {
            window.removeEventListener("mousemove", mouseMove)
            window.removeEventListener("mouseup", mouseUp)
        }
    }
}

@Composable
private fun ResizeHandle(
    resizing: Boolean,
    onResizeStart: (SyntheticMouseEvent) -> Unit,
) {
    Div({
        onMouseDown(onResizeStart)
        classes("group", "absolute", "-left-1", "top-0", "hidden", "h-full", "w-2", "cursor-col-resize", "place-items-center", "md:grid")
        if (resizing) {
            classes("bg-slate-700/30")
        } else {
            classes("hover:bg-slate-700/20")
        }
    }) {
        Div({
            classes("h-10", "w-1", "rounded-full", "bg-slate-600", "opacity-60", "transition", "group-hover:opacity-100")
        })
    }
}
