package de.mineking.hexo.web.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.hds.game.GameFinishReason
import de.mineking.hexo.hds.session.LiveSession
import de.mineking.hexo.hds.session.LiveSessionPlayer
import de.mineking.hexo.hds.session.SessionState
import de.mineking.hexo.web.components.Card
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.cssColor
import de.mineking.hexo.web.icons.ChevronDownIcon
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.time.Duration

private val springTransition = listOf(
    "duration-300",
    "ease-[cubic-bezier(0.34,1.56,0.64,1)]",
)

private val expandPositionTransition = listOf(
    "duration-300",
    "ease-[cubic-bezier(0.34,1.36,0.64,1)]",
)

private val collapsePositionTransition = listOf(
    "duration-300",
    "ease-[cubic-bezier(0.2,1.20,0.38,1)]",
)

@Composable
fun SessionFinishedOverlay(session: LiveSession, state: SessionState.Finished) {
    var collapsed by remember(session.id, state) { mutableStateOf(false) }

    Card({
        classes(
            "fixed", "bottom-4", "left-1/2", "z-50", "w-[calc(100%-2rem)]", "-translate-x-1/2", "overflow-hidden", "transition-all",
            "bg-slate-950/90!", "border-slate-700/90!", "backdrop-blur-sm", "border-2",
        )

        if (collapsed) {
            classes("max-w-lg", "max-h-14", "translate-y-0", "scale-95")
            classes(collapsePositionTransition)
        } else {
            classes("max-w-2xl", "max-h-[70vh]", "translate-y-[calc(50%-50vh+1rem)]", "scale-100")
            classes(expandPositionTransition)
        }
    }) {
        SessionFinishedOverlayHeader(
            collapsed = collapsed,
            onExpandCollapse = { collapsed = !collapsed },
        )
        SessionFinishedOverlayBody(collapsed, session, state)
    }
}

@Composable
private fun SessionFinishedOverlayHeader(
    collapsed: Boolean,
    onExpandCollapse: () -> Unit,
) {
    Button({
        classes(springTransition)
        classes(
            "group", "relative", "flex", "w-full", "items-center", "justify-center", "transition-all",
            "cursor-pointer", "select-none", "border-0", "bg-transparent", "text-slate-200", "hover:text-slate-50",
        )
        attr("aria-expanded", (!collapsed).toString())
        attr("aria-label", if (collapsed) "Expand finished session overlay" else "Collapse finished session overlay")
        onClick { onExpandCollapse() }

        if (collapsed) {
            classes("p-2")
        } else {
            classes("p-4", "pb-0")
        }
    }) {
        Span({ classes("text-2xl", "font-extrabold", "uppercase") }) {
            Text("Match finished")
        }

        ChevronDownIcon {
            classes(
                "absolute", "right-3", "size-5", "text-slate-400",
                "transition-all", "group-hover:text-slate-100",
            )
            classes(springTransition)
            if (collapsed) classes("rotate-180")
        }
    }
}

@Composable
private fun SessionFinishedOverlayBody(collapsed: Boolean, session: LiveSession, state: SessionState.Finished) {
    Div({
        classes("grid", "transition-all")
        classes(springTransition)

        if (collapsed) {
            classes("grid-rows-[0fr]", "opacity-0", "px-4", "pb-0")
        } else {
            classes("grid-rows-[1fr]", "opacity-100", "px-4", "pb-4", "mt-6")
        }
    }) {
        Div({ classes("min-h-0", "overflow-y-auto") }) {
            Div({ classes("grid", "gap-4") }) {
                SessionFinishedResultSummary(state)

                Div({ classes("grid", "gap-3", "sm:grid-cols-2") }) {
                    session.players.forEach {
                        SessionFinishedPlayerCard(it, session, state)
                    }
                }

                SessionFinishedTournamentSummary(session, state)
            }
        }
    }
}

@Composable
private fun SessionFinishedResultSummary(state: SessionState.Finished) {
    val winner = state.result.winner

    Div({
        classes("rounded-xl", "border", "border-slate-700/70", "bg-slate-900/75", "px-4", "py-3", "text-center")
    }) {
        P({ classes("text-xs", "font-semibold", "uppercase", "tracking-wide", "text-slate-400") }) {
            Text(state.result.reason.label)
        }
        P({
            classes("mt-1", "text-xl", "font-bold")
            classes(if (winner == null) "text-amber-200" else "text-slate-100")
        }) {
            Text(winner?.let { "${it.displayName} wins" } ?: "Draw")
        }
    }
}

@Composable
private fun SessionFinishedPlayerCard(player: LiveSessionPlayer, session: LiveSession, state: SessionState.Finished) {
    SubCard({
        classes("p-3")
        when (state.result.winner) {
            null -> classes("bg-slate-700/60!")
            player -> classes("bg-emerald-500/20!", "border-emerald-500/60!")
            else -> classes("bg-rose-400/20!", "border-rose-400/60!")
        }
    }) {
        H2({ classes("text-lg", "text-slate-200", "font-semibold", "flex", "items-center") }) {
            Div({
                classes("rounded-full", "size-3", "mr-1.5", "shrink-0")
                style { backgroundColor(player.color.cssColor) }
            })
            Text(player.displayName)
            if (player in state.rematchAcceptedPlayers) {
                SessionFinishedBadge(
                    label = "Rematch",
                    badgeClasses = listOf("bg-sky-500/20", "text-sky-200", "ml-auto"),
                )
            }
        }

        Div({ classes("mt-3", "flex", "items-center", "justify-between", "gap-3") }) {
            Span({ classes("text-sm", "font-medium", "text-slate-300") }) {
                Text("Elo ${player.elo}")
                val eloAdjustment = player.eloAdjustment.takeIf { session.gameOptions.rated }
                if (eloAdjustment != null) {
                    Span({
                        classes("ml-1")
                        classes(if (player == state.result.winner) "text-emerald-300" else "text-rose-300")
                    }) {
                        Text(if (player == state.result.winner) "+${eloAdjustment.eloGain}" else "${eloAdjustment.eloLoss}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionFinishedTournamentSummary(session: LiveSession, state: SessionState.Finished) {
    val tournament = session.tournamentInfo ?: return
    if (tournament.bestOf < 3) return
    val requiredWins = tournament.bestOf / 2 + 1

    Div({
        classes("rounded-xl", "border", "border-slate-700/70", "bg-slate-800/80", "px-4", "py-3")
    }) {
        Div({ classes("mb-3", "flex", "items-center", "justify-between", "gap-3") }) {
            P({ classes("min-w-0", "truncate", "text-sm", "font-semibold", "text-slate-200") }) {
                Text(tournament.tournamentName)
            }
            Span({ classes("shrink-0", "rounded-full", "bg-slate-900/70", "px-2", "py-0.5", "text-xs", "font-semibold", "text-slate-400") }) {
                Text("Game ${tournament.currentGameNumber} of ${tournament.bestOf}")
            }
        }

        Div({ classes("grid", "gap-4") }) {
            session.players.forEach { player ->
                val matchWins = player.tournamentMatchWins
                    ?.let { if (player == state.result.winner) it + 1 else it } // The API doesn't send an updated value after the game finishes
                    ?: 0
                val progress = (matchWins * 100 / requiredWins).coerceIn(0, 100)

                Div({ classes("grid", "grid-cols-[minmax(0,1fr)_auto]", "items-center", "gap-3") }) {
                    Div({ classes("min-w-0") }) {
                        Div({ classes("mb-1", "flex", "items-center", "gap-1.5") }) {
                            Div({
                                classes("rounded-full", "size-2", "shrink-0")
                                style { backgroundColor(player.color.cssColor) }
                            })
                            Span({ classes("truncate", "text-xs", "font-medium", "text-slate-300") }) {
                                Text(player.displayName)
                            }
                        }
                        Div({ classes("h-1.5", "overflow-hidden", "rounded-full", "bg-slate-950/70") }) {
                            Div({
                                classes("h-full", "rounded-full")
                                classes(if (matchWins > 0) "bg-emerald-500" else "bg-slate-700")
                                style { width(progress.percent) }
                            })
                        }
                    }
                    Span({ classes("inline-flex", "w-12", "items-baseline", "justify-end", "font-semibold", "tabular-nums") }) {
                        Span({
                            classes(if (matchWins == requiredWins) "text-emerald-300" else "text-slate-300")
                        }) {
                            Text(matchWins.toString())
                        }
                        Span({ classes("text-slate-500", "text-sm") }) {
                            Text("/$requiredWins")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionFinishedBadge(label: String, badgeClasses: List<String>) {
    Span({
        classes("rounded-full", "px-2", "py-0.5", "text-xs", "font-semibold")
        classes(badgeClasses)
    }) {
        Text(label)
    }
}

private val GameFinishReason.label get() = when (this) {
    GameFinishReason.SixInARow -> "Six in a Row"
    GameFinishReason.Timeout -> "Timeout"
    GameFinishReason.Surrender -> "Surrender"
    GameFinishReason.Disconnect -> "Disconnect"
    GameFinishReason.DrawAgreement -> "Draw agreed"
    GameFinishReason.Terminated -> "Terminated"
}

private fun Duration.formatCompact(): String {
    val hours = inWholeHours
    val minutes = inWholeMinutes % 60
    val seconds = inWholeSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
