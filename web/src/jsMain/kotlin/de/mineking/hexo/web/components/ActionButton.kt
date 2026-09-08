@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLButtonElement

enum class ButtonSize {
    Small,
    Medium,
}

@Composable
fun ActionButton(
    tooltip: String,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.Small,
    color: Color = Color.Neutral,
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val onClick by rememberUpdatedState(onClick)
    Tooltip(
        text = tooltip,
        attrs = { classes("inline-flex", "shrink-0") },
        tooltipAttrs = { classes("text-left") },
        showArrow = false,
    ) {
        Button({
            classes("rounded-lg", "border", "text-nowrap", "font-medium", "transition")

            if (!enabled) disabled()
            if (enabled) classes("cursor-pointer")

            when (size) {
                ButtonSize.Small -> classes("px-2.5", "py-1", "text-xs")
                ButtonSize.Medium -> classes("px-4", "py-1.5", "text-sm")
            }

            colorButtonClasses(color, enabled)
            attrs?.invoke(this)
            attr("aria-label", tooltip)
            onClick { onClick() }
        }) {
            content()
        }
    }
}

@Composable
fun ActionButton(
    label: String,
    tooltip: String,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.Small,
    color: Color = Color.Neutral,
    onClick: () -> Unit,
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
) {
    ActionButton(
        tooltip = tooltip,
        enabled = enabled,
        size = size,
        color = color,
        attrs = attrs,
        onClick = onClick,
    ) { Text(label) }
}
