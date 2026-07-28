package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.web.attributes.builders.InputAttrsScope
import org.jetbrains.compose.web.dom.CheckboxInput

@Composable
fun Checkbox(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    attrs: InputAttrsScope<Boolean>.() -> Unit = {},
) {
    val onValueChange by rememberUpdatedState(onValueChange)
    CheckboxInput(value) {
        classes("hexo-checkbox")
        onChange { onValueChange(it.value) }
        attrs()
    }
}
