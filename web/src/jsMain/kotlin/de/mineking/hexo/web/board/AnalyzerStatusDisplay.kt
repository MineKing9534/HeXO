package de.mineking.hexo.web.board

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.borderColor
import de.mineking.hexo.hds.game.Player
import de.mineking.hexo.solver.FindDefenseResult
import de.mineking.hexo.solver.FindWinResult
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.icons.ShieldIcon
import de.mineking.hexo.web.icons.SwordIcon
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement

@Composable
fun AnalyzerStatusDisplay(
    state: BoardAnalyzerState,
    analyzedPlayer: Player,
    otherPlayer: Player,
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
                    LoadingIndicator { classes("size-4", "border-2!", "border-t-slate-300!") }
                },
                detail = { Text("Searching for forced wins") },
            )

            is BoardAnalyzerState.Data -> {
                AnalyzerResultDisplay(state, analyzedPlayer, otherPlayer)
            }
        }
        actions()
    }
}

@Composable
private fun AnalyzerResultDisplay(
    state: BoardAnalyzerState.Data,
    analyzedPlayer: Player,
    otherPlayer: Player,
) {
    if (state.threat is FindWinResult.Win) {
        AnalyzerNotificationCard(
            title = {
                de.mineking.hexo.web.session.Player(analyzedPlayer)
                Text(" has a forced win")
            },
            color = "emerald-400",
            icon = { SwordIcon { classes("size-5") } },
            detail = { Text("Winning sequence highlighted") },
        )
    }

    if (state.defense is FindDefenseResult.Threat) {
        AnalyzerNotificationCard(
            title = {
                de.mineking.hexo.web.session.Player(otherPlayer)
                Text(" threatens a forced win")
            },
            color = "rose-400",
            icon = { ShieldIcon { classes("size-5") } },
            detail = {
                Text(when {
                    state.threat is FindWinResult.Win -> "The other player can win before this win takes effect"
                    state.defense.defenses.isEmpty() -> "No defense found"
                    else -> "Potential defense highlighted"
                })
            },
            attrs = {
                if (state.threat is FindWinResult.Win) classes("opacity-75!", "grayscale-25")
            },
        )
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
            Span({ classes("mt-0.5", "block", "text-xs", "font-medium", "text-slate-400") }) {
                detail()
            }
        }
    }
}
