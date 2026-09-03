package de.mineking.hexo.web.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.render.image.theme.CellShape
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.web.components.Checkbox
import de.mineking.hexo.web.components.DropdownMenu
import de.mineking.hexo.web.components.HorizontalDivider
import de.mineking.hexo.web.components.Slider
import de.mineking.hexo.web.icons.CheckIcon
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun SettingsView() {
    Div({ classes("flex", "flex-col", "gap-3") }) {
        ThemeSettingsField()
        BooleanSettingsField(
            key = SettingsKey.ReadOnlyBoardHoverIndicator,
            title = "Read-only hover indicator",
            description = { Text("Show the target cell while hovering over boards you cannot edit.") },
        )
        BooleanSettingsField(
            key = SettingsKey.SessionAnalyzer,
            title = "Session analyzer",
            description = {
                Text("Analyzes sessions for forced-wins while watching (Powered by ")
                A(href = "https://github.com/SootyOwl/hexo-strix", {
                    target(ATarget.Blank)
                    classes("font-bold", "text-sky-400")
                }) {
                    Text("Strix")
                }
                Text(").")
            },
        )

        HorizontalDivider()

        BooleanSettingsField(
            key = SettingsKey.SessionViewTimerSounds,
            title = "Timeout sounds",
            description = { Text("Play an alert when a watched session timer runs out.") },
        )
        FloatSettingsField(
            key = SettingsKey.Volume,
            title = "Volume",
            description = { Text("Adjust the volume of sound effects.") },
            valueLabel = { "${(it * 100).toInt()}%" },
        )
    }
}

@Composable
private fun ThemeSettingsField() {
    var theme by SettingsKey.Theme.collectAsState()

    Div({
        classes(
            "grid", "gap-3", "rounded-lg", "border", "border-slate-800", "bg-slate-950/40",
            "p-3",
        )
    }) {
        Div({ classes("grid", "gap-1") }) {
            Span({ classes("text-sm", "font-semibold", "text-slate-200") }) { Text("Board theme") }
            Span({ classes("text-sm", "leading-snug", "text-slate-500") }) {
                Text("Choose the visual style used to render boards.")
            }
        }
        DropdownMenu(label = {
            Div({ classes("flex", "min-w-0", "items-center", "gap-2.5") }) {
                ThemePreview(theme, compact = true)
                Text(theme.displayName)
            }
        }) { close ->
            DefaultTheme.entries.forEach { entry ->
                ThemeOption(entry, selected = entry == theme) {
                    theme = entry
                    close()
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(theme: DefaultTheme, selected: Boolean, onClick: () -> Unit) {
    Button({
        classes(
            "flex", "w-full", "cursor-pointer", "items-center", "justify-between", "rounded-lg", "border",
            "px-3", "py-2.5", "text-left", "text-sm", "font-semibold", "transition",
        )
        if (selected) {
            classes("border-emerald-400/25", "bg-emerald-500/10", "text-emerald-200")
        } else {
            classes("border-transparent", "text-slate-400", "hover:bg-slate-800", "hover:text-slate-100")
        }
        onClick { onClick() }
    }) {
        Div({ classes("flex", "min-w-0", "items-center", "gap-3") }) {
            ThemePreview(theme)
            Text(theme.displayName)
        }
        if (selected) CheckIcon { classes("size-4", "shrink-0") }
    }
}

@Composable
private fun ThemePreview(theme: DefaultTheme, compact: Boolean = false) {
    val palette = theme.theme
    Div({
        classes(
            "relative", "shrink-0", "overflow-hidden", "border", "border-white/10", "shadow-inner",
            if (compact) "h-5" else "h-8",
            if (compact) "w-9" else "w-14",
            if (compact) "rounded" else "rounded-md",
        )
        style { backgroundColor(palette.backgroundColor.css) }
    }) {
        PreviewCell(theme, palette.playerXColor, first = true, compact = compact)
        PreviewCell(theme, palette.playerOColor, first = false, compact = compact)
    }
}

@Composable
private fun PreviewCell(theme: DefaultTheme, color: Color, first: Boolean, compact: Boolean) {
    Div({
        classes(
            "absolute",
            if (compact) "size-2.5" else "size-4",
            if (first) "left-[18%]" else "right-[18%]",
            if (first) "top-[18%]" else "bottom-[18%]",
            if (theme.theme.cellShape == CellShape.Circle) "rounded-full" else "[clip-path:polygon(25%_7%,75%_7%,100%_50%,75%_93%,25%_93%,0_50%)]",
        )
        style { backgroundColor(color.css) }
    })
}

private val Color.css get() = rgba(red, green, blue, alpha)

private val DefaultTheme.displayName get() = when (this) {
    DefaultTheme.HDS -> "HDS"
    DefaultTheme.HTTTX -> "HTTTX"
    DefaultTheme.Tyto -> "Tyto"
    DefaultTheme.Omok -> "Omok"
}

@Composable
private fun FloatSettingsField(
    key: SettingsKey<Float>,
    title: String,
    description: @Composable () -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float = 0.01f,
    valueLabel: (Float) -> String = { it.toString() },
) {
    var value by key.collectAsState()

    Div({
        classes(
            "flex", "flex-col", "gap-3", "rounded-lg", "border", "border-slate-800", "bg-slate-950/40",
            "p-3",
        )
    }) {
        Div({ classes("flex", "items-center", "justify-between", "gap-3") }) {
            Span({ classes("text-sm", "font-semibold", "text-slate-200") }) { Text(title) }
            Span({ classes("text-sm", "font-medium", "tabular-nums", "text-slate-400") }) {
                Text(valueLabel(value))
            }
        }
        Slider(value = value, onValueChange = { value = it }, range = range, step = step) {
            attr("aria-label", title)
        }
        Span({ classes("text-sm", "leading-snug", "text-slate-500") }) {
            description()
        }
    }
}

@Composable
fun BooleanSettingsField(
    key: SettingsKey<Boolean>,
    title: String,
    description: @Composable () -> Unit,
) {
    var value by key.collectAsState()
    val checkboxId = "setting-${key.name}"

    Label(forId = checkboxId, attrs = {
        classes(
            "group", "flex", "cursor-pointer", "select-none", "items-start", "gap-3", "rounded-lg", "border",
            "border-slate-800", "bg-slate-950/40", "p-3", "text-left", "transition",
            "hover:border-slate-700", "hover:bg-slate-800/50",
        )
    }) {
        Checkbox(value = value, onValueChange = { value = it }) {
            attr("id", checkboxId)
            classes("mt-0.5", "size-5")
        }
        Span({ classes("min-w-0", "flex", "flex-col", "gap-1") }) {
            Span({
                classes(
                    "text-sm", "font-semibold", "leading-tight", "text-slate-200", "transition-colors",
                    "group-hover:text-slate-100",
                )
            }) {
                Text(title)
            }
            Span({ classes("text-sm", "leading-snug", "text-slate-500") }) {
                description()
            }
        }
    }
}
