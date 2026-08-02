package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.web.attributes.builders.InputAttrsScope
import org.jetbrains.compose.web.dom.RangeInput

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float = 0.01f,
    attrs: InputAttrsScope<Number?>.() -> Unit = {},
) {
    val onValueChange by rememberUpdatedState(onValueChange)
    val progress = if (range.start == range.endInclusive) {
        0f
    } else {
        ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f) * 100
    }

    RangeInput(
        value = value,
        min = range.start,
        max = range.endInclusive,
        step = step,
    ) {
        classes("hexo-slider")
        style { property("--hexo-slider-progress", "$progress%") }
        onInput { event ->
            event.value?.let { onValueChange(it.toFloat()) }
        }
        attrs()
    }
}
