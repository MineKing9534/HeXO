package de.mineking.hexo.web.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import de.mineking.hexo.web.components.Checkbox
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun SettingsView() {
    Div({ classes("flex", "flex-col", "gap-3") }) {
        BooleanSettingsField(
            key = SettingsKey.SessionViewTimerSounds,
            title = "Timeout sounds",
            description = { Text("Play an alert when a watched session timer runs out.") },
        )
        BooleanSettingsField(
            key = SettingsKey.ReadOnlyBoardHoverIndicator,
            title = "Read-only hover indicator",
            description = { Text("Show the target cell while hovering over boards you cannot edit.") },
        )
        BooleanSettingsField(
            key = SettingsKey.SessionAnalyzer,
            title = "Session analyzer",
            description = {
                Text("Analyzed sessions for forced-wins while watching (Powered by ")
                A(href = "https://github.com/SootyOwl/hexo-strix", {
                    target(ATarget.Blank)
                    classes("font-bold", "text-sky-400")
                }) {
                    Text("Strix")
                }
                Text(").")
            },
        )
    }
}

@Composable
private fun BooleanSettingsField(
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
