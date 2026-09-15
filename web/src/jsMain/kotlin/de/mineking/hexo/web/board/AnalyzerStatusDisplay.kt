package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.borderColor
import de.mineking.hexo.solver.FindDefenseResult
import de.mineking.hexo.solver.FindWinResult
import de.mineking.hexo.solver.isDefendable
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.LoadingIndicatorSize
import de.mineking.hexo.web.icons.ChevronLeftIcon
import de.mineking.hexo.web.icons.ChevronRightIcon
import de.mineking.hexo.web.icons.ShieldIcon
import de.mineking.hexo.web.icons.SwordIcon
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement

@Composable
fun AnalyzerStatusDisplay(
    state: BoardAnalyzerState,
    analyzedPlayer: GamePlayer,
    otherPlayer: GamePlayer,
    selectedDefenseIndex: Int = 0,
    onSelectedDefenseIndexChange: (Int) -> Unit = {},
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
    actions: @Composable () -> Unit,
) {
    Div({
        classes("pointer-events-auto", "z-20", "flex", "flex-col", "items-end", "gap-2")
        attr("role", "status")
        attr("aria-live", "polite")
        attrs?.invoke(this)
    }) {
        when (state) {
            is BoardAnalyzerState.Loading -> AnalyzerNotificationCard(
                title = { Text("Analysing position...") },
                color = "slate-400",
                icon = {
                    LoadingIndicator(LoadingIndicatorSize.Tiny) { classes("border-t-slate-300!") }
                },
                detail = { Text("Searching for forced wins") },
            )

            is BoardAnalyzerState.Data -> {
                AnalyzerResultDisplay(
                    state,
                    analyzedPlayer,
                    otherPlayer,
                    selectedDefenseIndex,
                    onSelectedDefenseIndexChange,
                )
            }
        }
        actions()
    }
}

@Composable
private fun AnalyzerResultDisplay(
    state: BoardAnalyzerState.Data,
    analyzedPlayer: GamePlayer,
    otherPlayer: GamePlayer,
    selectedDefenseIndex: Int,
    onSelectedDefenseIndexChange: (Int) -> Unit,
) {
    if (state.threat is FindWinResult.Win) {
        AnalyzerNotificationCard(
            title = {
                PlayerName(analyzedPlayer)
                Text(" has a forced win")
            },
            color = "emerald-400",
            icon = { SwordIcon { classes("size-5") } },
            detail = { Text("Winning sequence highlighted") },
        )
    }

    if (state.defense is FindDefenseResult.Threat) {
        val defenses = state.defense.displayedDefenses()
        val selectedIndex = selectedDefenseIndex.coerceIn(0, maxOf(0, defenses.lastIndex))
        AnalyzerNotificationCard(
            title = {
                PlayerName(otherPlayer)
                Text(" threatens a forced win")
            },
            color = "rose-400",
            icon = { ShieldIcon { classes("size-5") } },
            detail = {
                when {
                    state.threat is FindWinResult.Win -> Text("The other player can win before this win takes effect")
                    !state.defense.isDefendable() -> Text("No defense found")
                    defenses.isNotEmpty() -> {
                        DefenseSelector(
                            selectedIndex = selectedIndex,
                            defenseCount = defenses.size,
                            onSelectedIndexChange = onSelectedDefenseIndexChange,
                        )
                    }
                }
            },
            attrs = {
                if (state.threat is FindWinResult.Win) classes("opacity-75!", "grayscale-25")
            },
        )
    }
}

@Composable
private fun DefenseSelector(
    selectedIndex: Int,
    defenseCount: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    Div({
        classes("flex", "items-center", "gap-2")
        attr("role", "group")
        attr("aria-label", "Select defense")
    }) {
        Span({
            classes("text-slate-400")
            attr("aria-label", "Showing defense ${selectedIndex + 1} of $defenseCount")
        }) {
            Text("Showing defense ")
            Span({ classes("font-semibold", "tabular-nums", "text-slate-200") }) {
                Text("${selectedIndex + 1}")
            }
            Span({ classes("mx-0.5") }) { Text("/") }
            Span({ classes("tabular-nums") }) { Text("$defenseCount") }
        }
        Div({
            classes("flex", "items-center", "gap-0.5", "ml-auto")
        }) {
            DefenseSelectorButton("Previous defense", selectedIndex > 0, {
                onSelectedIndexChange(selectedIndex - 1)
            }) {
                ChevronLeftIcon {
                    classes("size-3")
                    if (selectedIndex > 0) classes("transition-transform", "group-hover:-translate-x-px")
                }
            }
            DefenseSelectorButton("Next defense", selectedIndex < defenseCount - 1, {
                onSelectedIndexChange(selectedIndex + 1)
            }) {
                ChevronRightIcon {
                    classes("size-3")
                    if (selectedIndex < defenseCount - 1) classes("transition-transform", "group-hover:translate-x-px")
                }
            }
        }
    }
}

@Composable
private fun DefenseSelectorButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Button({
        classes(
            "group", "grid", "size-5", "place-items-center", "rounded", "border-0", "bg-transparent", "transition",
            "focus:outline-none", "focus-visible:z-10", "focus-visible:ring-2", "focus-visible:ring-inset",
            "focus-visible:ring-emerald-400/70",
        )
        if (enabled) {
            classes("cursor-pointer", "text-slate-400", "hover:bg-white/5", "hover:text-slate-100")
        } else {
            classes("text-slate-600")
            disabled()
        }
        attr("aria-label", label)
        onClick { onClick() }
    }) {
        content()
    }
}

@Composable
private fun AnalyzerNotificationCard(
    title: @Composable () -> Unit,
    color: String,
    icon: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    attrs: AttrBuilderContext<HTMLDivElement>? = null,
) {
    Div({
        classes(
            "flex", "min-w-64", "items-center", "gap-3", "rounded-md", "border", "border-(--color)",
            "px-3", "py-2.5", "shadow-xl", "shadow-black/35", "backdrop-blur-md", "opacity-90",
        )
        style {
            property("--color", "var(--color-$color)")
            backgroundColor(Color("color-mix(in srgb, var(--color) 12%, rgb(2 6 23 / 94%))"))
            borderColor(Color("color-mix(in srgb, var(--color) 65%, rgb(51 65 85))"))
        }
        attrs?.invoke(this)
    }) {
        Div({
            classes("grid", "size-8", "shrink-0", "place-items-center", "rounded-md", "text-(--color)")
            style {
                backgroundColor(Color("color-mix(in srgb, var(--color) 18%, transparent)"))
            }
        }) {
            icon()
        }

        Div({ classes("min-w-0", "leading-tight") }) {
            Span({ classes("block", "text-sm", "font-bold", "text-slate-100") }) {
                title()
            }
            Div({ classes("mt-0.5", "text-xs", "font-medium", "text-slate-400") }) {
                detail()
            }
        }
    }
}
