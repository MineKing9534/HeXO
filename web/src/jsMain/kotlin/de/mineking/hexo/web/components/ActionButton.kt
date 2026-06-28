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
    label: String,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.Small,
    color: Color = Color.Neutral,
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
    onClick: () -> Unit,
) {
    val onClick by rememberUpdatedState(onClick)
    Button({
        classes("rounded-md", "border", "text-nowrap", "font-medium", "transition")

        if (!enabled) disabled()
        if (enabled) classes("cursor-pointer")

        when (size) {
            ButtonSize.Small -> classes("px-2.5", "py-1", "text-xs")
            ButtonSize.Medium -> classes("px-4", "py-2", "text-sm")
        }

        colorButtonClasses(color, enabled)
        attrs?.invoke(this)
        onClick { onClick() }
    }) {
        Text(label)
    }
}
