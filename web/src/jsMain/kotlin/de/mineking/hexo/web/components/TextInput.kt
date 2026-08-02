package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.builders.InputAttrsScope
import org.jetbrains.compose.web.attributes.builders.TextAreaAttrsScope
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.TextArea

@Composable
fun TextInput(
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    type: InputType<String> = InputType.Text,
    placeholder: String? = null,
    valid: Boolean? = null,
    readOnly: Boolean = false,
    monospace: Boolean = false,
    attrs: (InputAttrsScope<String>.() -> Unit)? = null,
) {
    Input(type) {
        value(value)
        if (placeholder != null) placeholder(placeholder)
        if (readOnly) readOnly()
        if (onValueChange != null) onInput { onValueChange(it.value) }
        fieldClasses(valid, monospace)
        attrs?.invoke(this)
    }
}

@Composable
fun TextAreaInput(
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    placeholder: String? = null,
    valid: Boolean? = null,
    readOnly: Boolean = false,
    monospace: Boolean = false,
    attrs: (TextAreaAttrsScope.() -> Unit)? = null,
) {
    TextArea {
        value(value)
        if (placeholder != null) placeholder(placeholder)
        if (readOnly) readOnly()
        if (onValueChange != null) onInput { onValueChange(it.value) }
        fieldClasses(valid, monospace)
        attrs?.invoke(this)
    }
}

private fun AttrsScope<*>.fieldClasses(valid: Boolean?, monospace: Boolean) {
    classes(
        "w-full", "rounded-lg", "border-2", "border-slate-700", "bg-slate-950", "p-3",
        "text-sm", "text-slate-100", "placeholder-slate-500", "outline-none", "transition",
        "focus:bg-slate-800",
    )
    if (monospace) classes("font-mono")
    when (valid) {
        true -> classes("focus:border-emerald-400")
        false -> classes("focus:border-rose-400")
        null -> classes("focus:border-emerald-400")
    }
}
