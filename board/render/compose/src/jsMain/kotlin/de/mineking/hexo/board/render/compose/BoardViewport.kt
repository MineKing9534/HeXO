package de.mineking.hexo.board.render.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.render.image.BoardRenderLayout
import de.mineking.hexo.board.render.image.Point
import de.mineking.hexo.board.render.image.div
import de.mineking.hexo.board.render.image.minus
import de.mineking.hexo.board.render.image.plus
import de.mineking.hexo.board.render.image.times
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import kotlin.math.sqrt
import kotlin.reflect.KProperty

internal const val BOARD_RENDER_PADDING = 128

private const val MIN_ZOOM = 0.07
private const val MAX_ZOOM = 0.8
private const val ZOOM_STEP = 1.1
private const val PINCH_ZOOM_DEAD_ZONE = 0.025
private const val MOUSE_DRAG_THRESHOLD = 4.0
private const val LONG_PRESS_DELAY = 450
private const val LONG_PRESS_MOVE_TOLERANCE = 12.0

private const val PRIMARY_BUTTON = 0
private const val MIDDLE_BUTTON = 1
private const val SECONDARY_BUTTON = 2

data class BoardViewport(val zoom: Double, val center: Point) {
    internal fun pan(delta: Point) = copy(center = center - delta / zoom)

    internal fun zoomAt(value: Double, anchor: Point, canvas: HTMLCanvasElement): BoardViewport {
        val newZoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM)

        val anchorOffset = anchor - Point(canvas.clientWidth / 2.0, canvas.clientHeight / 2.0)
        return copy(
            zoom = newZoom,
            center = center + (anchorOffset / zoom) - (anchorOffset / newZoom),
        )
    }

    internal fun offset(canvas: HTMLCanvasElement) = Point(
        canvas.clientWidth / 2.0 - BOARD_RENDER_PADDING,
        canvas.clientHeight / 2.0 - BOARD_RENDER_PADDING,
    ) - center * zoom
}

sealed interface BoardClickEvent {
    val modifiers: BoardModifierKeys
}

data class BoardLeftClickEvent(
    val coordinate: CellCoordinate,
    override val modifiers: BoardModifierKeys,
) : BoardClickEvent

data class BoardRightClickEvent(
    val from: CellCoordinate,
    val to: CellCoordinate,
    val phase: BoardRightClickPhase,
    override val modifiers: BoardModifierKeys,
) : BoardClickEvent

data class BoardModifierKeys(
    val ctrlKey: Boolean = false,
    val altKey: Boolean = false,
    val shiftKey: Boolean = false,
)

enum class BoardRightClickPhase {
    Drag,
    Commit,
    Abort,
}

@Composable
internal fun BoardInteractions(
    element: HTMLCanvasElement?,
    renderLayout: () -> BoardRenderLayout,
    viewport: () -> BoardViewport,
    onViewportChange: (BoardViewport) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
    onCellHoverChange: (CellCoordinate?) -> Unit,
    onCellClick: (BoardLeftClickEvent) -> Unit,
    onBoardRightClick: (BoardRightClickEvent) -> Unit,
) {
    val onViewportChange by rememberUpdatedState(onViewportChange)
    val onDraggingChange by rememberUpdatedState(onDraggingChange)
    val onCellHoverChange by rememberUpdatedState(onCellHoverChange)
    val onCellClick by rememberUpdatedState(onCellClick)
    val onBoardRightClick by rememberUpdatedState(onBoardRightClick)
    val currentRenderLayout by rememberUpdatedState(renderLayout)
    val currentViewport by rememberUpdatedState(viewport)

    DisposableEffect(element) {
        val canvas = element ?: return@DisposableEffect onDispose {}
        val listeners = BoardEventListeners(
            canvas = canvas,
            renderLayout = { currentRenderLayout() },
            viewport = { currentViewport() },
            onViewportChange = { onViewportChange(it) },
            onDraggingChange = { onDraggingChange(it) },
            onCellHoverChange = { onCellHoverChange(it) },
            onCellClick = { onCellClick(it) },
            onBoardRightClick = { onBoardRightClick(it) },
        )

        listeners.attach()
        onDispose { listeners.detach() }
    }
}

private operator fun <T> (() -> T).getValue(thisRef: Any?, property: KProperty<*>) = this()
private data class MutableFunctionDelegate<T>(val get: () -> T, val set: (T) -> Unit) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = get()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
}

private fun <T> (() -> T).withSetter(set: (T) -> Unit) = MutableFunctionDelegate(this, set)

private class BoardEventListeners(
    private val canvas: HTMLCanvasElement,
    renderLayout: () -> BoardRenderLayout,
    viewport: () -> BoardViewport,
    onViewportChange: (BoardViewport) -> Unit,
    private val onDraggingChange: (Boolean) -> Unit,
    private val onCellHoverChange: (CellCoordinate?) -> Unit,
    private val onCellClick: (BoardLeftClickEvent) -> Unit,
    private val onBoardRightClick: (BoardRightClickEvent) -> Unit,
) {
    private val renderLayout by renderLayout
    private var viewport by viewport.withSetter(onViewportChange)

    private val canvasListeners = listOf(
        ListenerRegistration("pointerdown", ::pointerDown),
        ListenerRegistration("pointermove", ::pointerMove),
        ListenerRegistration("pointerup", ::pointerUp),
        ListenerRegistration("pointercancel", ::finishDrag),
        ListenerRegistration("touchstart", ::touchStart),
        ListenerRegistration("touchmove", ::touchMove),
        ListenerRegistration("touchend", ::touchEnd),
        ListenerRegistration("touchcancel", ::finishTouch),
        ListenerRegistration("click", ::click),
        ListenerRegistration("wheel", ::wheel),
        ListenerRegistration("contextmenu", ::suppressContextMenu),
        ListenerRegistration("pointerleave", ::leaveCanvas),
    )
    private val windowListeners = listOf(
        ListenerRegistration("blur", ::finishDrag),
        ListenerRegistration("blur", ::finishTouch),
        ListenerRegistration("blur", ::clearHover),
        ListenerRegistration("keydown", ::keyChange),
        ListenerRegistration("keyup", ::keyChange),
    )

    private var rightClickDrag: RightClickDrag? = null
    private var currentModifierKeys = BoardModifierKeys()
    private var mouseDownPosition: Point? = null
    private var lastDragPosition: Point? = null
        set(value) {
            val wasDragging = field != null
            val isDragging = value != null
            if (wasDragging != isDragging) onDraggingChange(isDragging)

            field = value
        }

    private var touchDragPosition: Point? = null
    private var touchPinchGesture: PinchGesture? = null
    private var touchLongPress: LongPress? = null
    private var touchMoved = false

    private var suppressNextClick = false
    private var hoveredCell: CellCoordinate? = null
        set(value) {
            if (field == value) return
            field = value
            onCellHoverChange(value)
        }

    private fun pointerDown(event: Event) {
        require(event is MouseEvent)
        if (event.isTouchPointer()) return

        syncModifierKeys(event)
        event.preventDefault()

        hoveredCell = null

        when (event.button.toInt()) {
            PRIMARY_BUTTON, MIDDLE_BUTTON -> beginMouseDrag(event)
            SECONDARY_BUTTON -> beginRightClickDrag(event)
        }
    }

    private fun pointerMove(event: Event) {
        require(event is MouseEvent)
        if (event.isTouchPointer()) return

        val modifiersChanged = updateModifierKeys(event)
        event.preventDefault()

        when {
            mouseDownPosition != null -> dragMouseTo(event.position())
            rightClickDrag != null -> dragRightClickTo(event.position(), force = modifiersChanged)
            else -> hoveredCell = cellAt(event.position())
        }
    }

    private fun pointerUp(event: Event) {
        require(event is MouseEvent)
        if (event.isTouchPointer()) return

        syncModifierKeys(event)
        if (event.button.toInt() == SECONDARY_BUTTON) {
            commitRightClick(event.position())
        }

        finishDrag(event)
        hoveredCell = cellAt(event.position())
    }

    private fun touchStart(event: Event) {
        val touches = event.touches()
        if (touches.isEmpty()) return

        suppressNextClick = false
        touchMoved = false
        hoveredCell = null
        finishMouseInteractions()

        if (touches.size >= 2) {
            event.preventDefault()
            cancelLongPress()
            beginTouchPinch(touches)
        } else {
            touchPinchGesture = null
            touchDragPosition = touches.first()
            scheduleLongPress(touches.first())
        }
    }

    private fun touchMove(event: Event) {
        val touches = event.touches()
        if (touches.isEmpty()) return

        if (touches.size >= 2) {
            event.preventDefault()
            cancelLongPress()
            if (touchPinchGesture == null) beginTouchPinch(touches)
            pinchTo(touches)
        } else {
            touchPinchGesture = null
            if (moveSingleTouchTo(touches.first())) {
                event.preventDefault()
            }
        }
    }

    private fun touchEnd(event: Event) {
        val touches = event.touches()

        val longPress = touchLongPress
        if (longPress?.active == true) {
            triggerLongPressRightClick(longPress, longPress.lastPosition, BoardRightClickPhase.Commit)
        }

        cancelLongPress()
        touchPinchGesture = null
        touchDragPosition = touches.singleOrNull()
        if (touches.isEmpty() && touchMoved) suppressNextClick = true
    }

    private fun click(event: Event) {
        require(event is MouseEvent)

        if (suppressNextClick) {
            suppressNextClick = false
            return
        }

        onCellClick(BoardLeftClickEvent(
            coordinate = cellAt(event.position()),
            modifiers = currentModifierKeys,
        ))
    }

    private fun wheel(event: Event) {
        event.preventDefault()
        require(event is WheelEvent)

        zoomAt(event)
    }

    private fun suppressContextMenu(event: Event) {
        event.preventDefault()
    }

    private fun keyChange(event: Event) {
        require(event is KeyboardEvent)

        suppressAltMenuDuringRightClick(event)
        if (updateModifierKeys(event)) refreshRightClickDrag()
    }

    private fun clearHover(@Suppress("unused") event: Event) {
        hoveredCell = null
    }

    private fun leaveCanvas(event: Event) {
        abortRightClick()
        clearHover(event)
    }

    fun attach() {
        canvasListeners.forEach { canvas.addEventListener(it.type, it.listener) }
        windowListeners.forEach { window.addEventListener(it.type, it.listener) }
    }

    fun detach() {
        canvasListeners.forEach { canvas.removeEventListener(it.type, it.listener) }
        windowListeners.forEach { window.removeEventListener(it.type, it.listener) }
        finishMouseInteractions()
        finishTouch()
        hoveredCell = null
    }

    private fun beginMouseDrag(event: MouseEvent) {
        suppressNextClick = false
        beginDrag(event)
    }

    private fun beginRightClickDrag(event: MouseEvent) {
        rightClickDrag = RightClickDrag(start = cellAt(event.position()))
    }

    private fun dragRightClickTo(to: Point, force: Boolean = false) {
        val drag = rightClickDrag ?: return

        val end = cellAt(to)
        if (end == drag.lastEnd && !force) return

        emitRightClick(drag.start, end, BoardRightClickPhase.Drag)
        rightClickDrag = drag.copy(lastEnd = end)
    }

    private fun commitRightClick(to: Point) {
        val drag = rightClickDrag ?: return

        emitRightClick(drag.start, cellAt(to), BoardRightClickPhase.Commit)
        rightClickDrag = null
    }

    private fun abortRightClick() {
        val drag = rightClickDrag ?: return

        emitRightClick(drag.start, drag.start, BoardRightClickPhase.Abort)
        rightClickDrag = null
    }

    private fun dragMouseTo(position: Point) {
        val startPosition = mouseDownPosition ?: return
        val lastPosition = lastDragPosition

        if (lastPosition == null && startPosition.distanceTo(position) <= MOUSE_DRAG_THRESHOLD) return

        val from = lastPosition ?: startPosition
        viewport = viewport.pan(position - from)
        suppressNextClick = true
        lastDragPosition = position
    }

    private fun dragTouchTo(position: Point): Boolean {
        val lastPosition = touchDragPosition ?: run {
            touchDragPosition = position
            return false
        }

        viewport = viewport.pan(position - lastPosition)
        suppressNextClick = true
        touchMoved = true
        touchDragPosition = position
        return true
    }

    private fun moveSingleTouchTo(position: Point): Boolean {
        val longPress = touchLongPress ?: return dragTouchTo(position)
        longPress.lastPosition = position

        return when {
            longPress.active -> {
                triggerLongPressRightClick(longPress, position, BoardRightClickPhase.Drag)
                true
            }

            longPress.startPosition.distanceTo(position) > LONG_PRESS_MOVE_TOLERANCE -> {
                cancelLongPress()
                dragTouchTo(position)
            }

            else -> false
        }
    }

    private fun scheduleLongPress(position: Point) {
        cancelLongPress()

        val timeout = window.setTimeout({
            val longPress = touchLongPress ?: return@setTimeout
            longPress.active = true
            suppressNextClick = true
            touchMoved = true
            touchDragPosition = null
            triggerLongPressRightClick(longPress, longPress.lastPosition, BoardRightClickPhase.Drag)
        }, LONG_PRESS_DELAY)

        touchLongPress = LongPress(
            startPosition = position,
            lastPosition = position,
            timeout = timeout,
        )
    }

    private fun cancelLongPress() {
        val longPress = touchLongPress ?: return
        window.clearTimeout(longPress.timeout)
        touchLongPress = null
    }

    private fun triggerLongPressRightClick(longPress: LongPress, position: Point, phase: BoardRightClickPhase) {
        val end = cellAt(position)
        if (end == longPress.lastCell && phase == BoardRightClickPhase.Drag) return

        emitRightClick(cellAt(longPress.startPosition), end, phase)
        longPress.lastCell = end
    }

    private fun refreshRightClickDrag() {
        val drag = rightClickDrag ?: return
        val end = drag.lastEnd ?: return
        emitRightClick(drag.start, end, BoardRightClickPhase.Drag)
    }

    private fun suppressAltMenuDuringRightClick(event: KeyboardEvent) {
        if (rightClickDrag != null && (event.altKey || event.key == "Alt")) event.preventDefault()
    }

    private fun emitRightClick(start: CellCoordinate, end: CellCoordinate, phase: BoardRightClickPhase) {
        onBoardRightClick(
            BoardRightClickEvent(
                from = start,
                to = end,
                phase = phase,
                modifiers = currentModifierKeys,
            ),
        )
    }

    private fun updateModifierKeys(event: MouseEvent) = updateModifierKeys(event.modifierKeys())

    private fun updateModifierKeys(event: KeyboardEvent) = updateModifierKeys(event.modifierKeys())

    private fun updateModifierKeys(modifiers: BoardModifierKeys): Boolean {
        if (currentModifierKeys == modifiers) return false
        currentModifierKeys = modifiers
        return true
    }

    private fun syncModifierKeys(event: MouseEvent) {
        currentModifierKeys = event.modifierKeys()
    }

    private fun MouseEvent.modifierKeys() = BoardModifierKeys(
        ctrlKey = ctrlKey,
        altKey = altKey,
        shiftKey = shiftKey,
    )

    private fun KeyboardEvent.modifierKeys() = BoardModifierKeys(
        ctrlKey = ctrlKey,
        altKey = altKey,
        shiftKey = shiftKey,
    )

    private fun finishMouseInteractions() {
        mouseDownPosition = null
        lastDragPosition = null
        rightClickDrag = null
    }

    private fun beginTouchPinch(touches: List<Point>) {
        touchDragPosition = null
        touchPinchGesture = touchPinch(touches)?.let { PinchGesture(start = it, viewport = viewport) }
        suppressNextClick = true
    }

    private fun pinchTo(touches: List<Point>) {
        val gesture = touchPinchGesture ?: touchPinch(touches)
            ?.let { PinchGesture(start = it, viewport = viewport) }
            ?.also { touchPinchGesture = it }
            ?: return

        val current = touchPinch(touches) ?: return

        val rect = canvas.getBoundingClientRect()
        val scale = if (gesture.start.distance == 0.0) {
            1.0
        } else {
            (current.distance / gesture.start.distance).let {
                if (it in (1 - PINCH_ZOOM_DEAD_ZONE)..(1 + PINCH_ZOOM_DEAD_ZONE)) 1.0 else it
            }
        }

        viewport = gesture.viewport
            .zoomAt(
                value = gesture.viewport.zoom * scale,
                anchor = gesture.start.center - Point(rect.left, rect.top),
                canvas = canvas,
            )
            .pan(current.center - gesture.start.center)

        suppressNextClick = true
        touchMoved = true
    }

    private fun touchPinch(touches: List<Point>): Pinch? {
        if (touches.size < 2) return null

        val delta = touches[0] - touches[1]
        return Pinch(
            center = (touches[0] + touches[1]) / 2.0,
            distance = sqrt(delta.x * delta.x + delta.y * delta.y),
        )
    }

    private fun beginDrag(event: MouseEvent) {
        mouseDownPosition = event.position()
        lastDragPosition = null
        capturePointer(event)
    }

    private fun capturePointer(event: Event) {
        val pointerId = event.asDynamic().pointerId ?: return
        canvas.setPointerCapture(pointerId)
    }

    private fun finishDrag(event: Event) {
        mouseDownPosition = null
        lastDragPosition = null
        releasePointer(event)
    }

    private fun finishTouch(@Suppress("unused") event: Event? = null) {
        cancelLongPress()
        touchDragPosition = null
        touchPinchGesture = null
        touchMoved = false
    }

    private fun releasePointer(event: Event) {
        val pointerId = event.asDynamic().pointerId ?: return
        if (canvas.hasPointerCapture(pointerId)) {
            canvas.releasePointerCapture(pointerId)
        }
    }

    private fun zoomAt(event: WheelEvent) {
        val position = event.position()
        val rect = canvas.getBoundingClientRect()

        viewport = viewport.zoomAt(
            value = viewport.zoom * if (event.deltaY < 0) ZOOM_STEP else 1 / ZOOM_STEP,
            anchor = position - Point(rect.left, rect.top),
            canvas = canvas,
        )
    }

    private fun cellAt(position: Point): CellCoordinate {
        val rect = canvas.getBoundingClientRect()

        val canvasTopLeft = Point(rect.left + BOARD_RENDER_PADDING, rect.top + BOARD_RENDER_PADDING)
        val point = position - canvasTopLeft - viewport.offset(canvas)
        return renderLayout.run { point.toCoordinate() }
    }

    private fun MouseEvent.position() = Point(clientX.toDouble(), clientY.toDouble())

    private fun Event.isTouchPointer() = asDynamic().pointerType == "touch"

    private fun Event.touches(): List<Point> {
        val touches = asDynamic().touches
        val length = (touches.length as Number).toInt()

        return List(length) {
            val touch = touches.item(it)
            Point(
                (touch.clientX as Number).toDouble(),
                (touch.clientY as Number).toDouble(),
            )
        }
    }

    private data class Pinch(val center: Point, val distance: Double)
    private data class PinchGesture(val start: Pinch, val viewport: BoardViewport)
    private data class ListenerRegistration(val type: String, val listener: (Event) -> Unit)
    private data class RightClickDrag(val start: CellCoordinate, val lastEnd: CellCoordinate? = null)
    private data class LongPress(
        val startPosition: Point,
        var lastPosition: Point,
        val timeout: Int,
        var active: Boolean = false,
        var lastCell: CellCoordinate? = null,
    )
}

private fun Point.distanceTo(point: Point): Double {
    val delta = this - point
    return sqrt(delta.x * delta.x + delta.y * delta.y)
}
