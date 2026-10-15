package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CoordinateTransform
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.plus
import de.mineking.hexo.board.render.compose.BoardViewport
import de.mineking.hexo.web.components.Popover
import de.mineking.hexo.web.components.PopoverAlignment
import de.mineking.hexo.web.components.PopoverPlacement
import de.mineking.hexo.web.icons.MirrorHorizontalIcon
import de.mineking.hexo.web.icons.MirrorVerticalIcon
import de.mineking.hexo.web.icons.RotateLeftIcon
import de.mineking.hexo.web.icons.RotateRightIcon
import de.mineking.hexo.web.icons.TransformBoardIcon
import de.mineking.hexo.web.icons.UndoIcon
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun BoardTransformationButton(viewport: BoardViewport, onViewportChange: (BoardViewport) -> Unit) {
    var rotation by remember { mutableStateOf(0) }
    var horizontal by remember { mutableStateOf(false) }
    var vertical by remember { mutableStateOf(false) }
    var selectionTransform by remember { mutableStateOf<CoordinateTransform?>(null) }

    LaunchedEffect(viewport.transform) {
        if (viewport.transform !== selectionTransform) {
            val unmirroredRotation = viewport.transform.rotationForMirror(false)
            val mirroredRotation = viewport.transform.rotationForMirror(true)

            rotation = unmirroredRotation ?: mirroredRotation ?: 0
            horizontal = unmirroredRotation == null && mirroredRotation != null
            vertical = false
            selectionTransform = viewport.transform
        }
    }

    fun update(nextRotation: Int = rotation, nextHorizontal: Boolean = horizontal, nextVertical: Boolean = vertical) {
        rotation = nextRotation
        horizontal = nextHorizontal
        vertical = nextVertical

        val transform = boardTransform(rotation, horizontal, vertical)
        selectionTransform = transform
        onViewportChange(viewport.copy(transform = transform))
    }

    Popover(
        label = "Board transformations",
        placement = PopoverPlacement.Top,
        alignment = PopoverAlignment.End,
        boundarySelector = "[data-board-pane]",
        popoverAttrs = { classes("@container/transform", "w-64") },
        trigger = { open, toggle -> BoardTransformTrigger(open, toggle) },
        content = {
            BoardTransformOptions(
                changed = rotation.mod(6) != 0 || horizontal || vertical,
                horizontal = horizontal,
                vertical = vertical,
                onRotate = { steps ->
                    val direction = if (horizontal xor vertical) -1 else 1
                    update(nextRotation = (rotation + steps * direction).mod(6))
                },
                onHorizontalChange = { update(nextHorizontal = !horizontal) },
                onVerticalChange = { update(nextVertical = !vertical) },
                onReset = { update(0, false, false) },
            )
        },
    )
}

@Composable
private fun BoardTransformTrigger(open: Boolean, onToggle: () -> Unit) {
    BoardActionButton(
        tooltip = "Transform board view",
        attrs = {
            attr("aria-haspopup", "dialog")
            attr("aria-expanded", open.toString())
        },
        onClick = onToggle,
    ) {
        TransformBoardIcon { classes("size-4") }
    }
}

private fun boardTransform(rotation: Int, horizontal: Boolean, vertical: Boolean): CoordinateTransform {
    var transform = if (rotation.mod(6) == 0) CoordinateTransform else CoordinateTransform.Rotation(rotation)
    if (horizontal) transform += CoordinateTransform.Mirror(Direction.Right)
    if (vertical) transform += CoordinateTransform.Mirror(Direction.Right) + CoordinateTransform.Rotation(3)
    return transform
}

private fun CoordinateTransform.rotationForMirror(horizontal: Boolean): Int? {
    val coordinates = listOf(CellCoordinate.Zero, Direction.Right.direction, Direction.BottomRight.direction)
    return (0 until 6).firstOrNull { rotation ->
        val candidate = boardTransform(rotation, horizontal, false)
        coordinates.all { transform(it) == candidate.transform(it) }
    }
}

@Composable
private fun BoardTransformOptions(
    changed: Boolean,
    horizontal: Boolean,
    vertical: Boolean,
    onRotate: (Int) -> Unit,
    onHorizontalChange: () -> Unit,
    onVerticalChange: () -> Unit,
    onReset: () -> Unit,
) {
    TransformationRow("Rotation") {
        TransformationAction("Rotate left by 60°", onClick = { onRotate(1) }) {
            RotateLeftIcon { classes("size-4", "shrink-0") }
            Text("Left")
        }
        TransformationAction("Rotate right by 60°", onClick = { onRotate(-1) }) {
            RotateRightIcon { classes("size-4", "shrink-0") }
            Text("Right")
        }
    }
    Div({ classes("mt-4") }) {
        TransformationRow("Mirrors") {
            TransformationAction("Mirror across horizontal axis", pressed = horizontal, onClick = onHorizontalChange) {
                MirrorHorizontalIcon { classes("size-4", "shrink-0") }
                Text("Horizontal")
            }
            TransformationAction("Mirror across vertical axis", pressed = vertical, onClick = onVerticalChange) {
                MirrorVerticalIcon { classes("size-4", "shrink-0") }
                Text("Vertical")
            }
        }
    }
    Div({ classes("-mx-3", "mt-3", "border-t", "border-slate-700/50", "px-3", "pt-3") }) {
        TransformationResetButton(enabled = changed, onReset = onReset)
    }
}

@Composable
private fun TransformationResetButton(enabled: Boolean, onReset: () -> Unit) {
    Button({
        classes(
            "flex", "h-8", "w-full", "items-center", "justify-center", "gap-2", "rounded-lg", "border", "px-2",
            "text-xs", "font-medium", "transition",
            "focus-visible:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
        )
        if (enabled) {
            classes(
                "cursor-pointer", "border-slate-700/50", "bg-slate-900/40", "text-slate-300",
                "hover:border-slate-600", "hover:bg-slate-800/70", "hover:text-slate-100",
            )
        } else {
            disabled()
            classes("cursor-default", "border-slate-800/70", "bg-slate-900/20", "text-slate-500")
        }
        attr("type", "button")
        attr("aria-label", "Reset rotation and mirrors")
        onClick { onReset() }
    }) {
        UndoIcon { classes("size-4", "shrink-0") }
        Text("Reset")
    }
}

@Composable
private fun TransformationRow(label: String, content: @Composable () -> Unit) {
    Div({
        classes(
            "mb-1.5", "flex", "h-6", "items-center", "text-[10px]", "font-semibold", "uppercase",
            "tracking-[0.12em]", "leading-4", "text-slate-400",
        )
    }) { Text(label) }
    Div({
        classes("grid", "grid-cols-2", "gap-2")
        attr("role", "group")
        attr("aria-label", label)
    }) { content() }
}

@Composable
private fun TransformationAction(
    label: String,
    pressed: Boolean? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Button({
        classes(
            "flex", "h-8", "min-w-0", "cursor-pointer", "items-center", "justify-center", "gap-2",
            "rounded-lg", "border", "px-2", "text-xs", "font-medium", "transition", "wrap-anywhere",
            "@max-[13rem]/transform:flex-col", "@max-[13rem]/transform:gap-1",
            "@max-[13rem]/transform:h-auto", "@max-[13rem]/transform:py-2",
            "focus-visible:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
        )
        if (pressed == true) {
            classes("border-emerald-400/50", "bg-emerald-500/15", "text-emerald-200", "hover:bg-emerald-500/25")
        } else {
            classes(
                "border-slate-700/50", "bg-slate-900/40", "text-slate-300",
                "hover:border-slate-600", "hover:bg-slate-800/70", "hover:text-slate-100",
            )
        }
        attr("type", "button")
        attr("aria-label", label)
        if (pressed != null) attr("aria-pressed", pressed.toString())
        onClick { onClick() }
    }) { content() }
}
