package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.game.model.game.GameFinishReason
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.session.LiveSession
import de.mineking.hexo.game.model.session.LiveSessionPlayer
import de.mineking.hexo.game.model.session.RatingAdjustment
import de.mineking.hexo.game.model.session.SessionState
import de.mineking.hexo.game.model.tournament.requiredWins
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.Card
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.formatCompact
import de.mineking.hexo.web.icons.ChevronDownIcon
import de.mineking.hexo.web.playerCssColor
import de.mineking.hexo.web.rememberTheme
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

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
            "fixed", "bottom-3", "left-1/2", "z-50", "w-[calc(100%-3rem)]", "-translate-x-1/2", "overflow-hidden",
            "transition-all", "sm:bottom-5",
            "bg-slate-950/92!", "border-slate-700/80!", "backdrop-blur-md", "border",
            "shadow-2xl", "shadow-black/45", "ring-1", "ring-white/5",
        )

        if (collapsed) {
            classes("max-w-xl", "max-h-16", "translate-y-0", "scale-95")
            classes(collapsePositionTransition)
        } else {
            classes("max-w-3xl", "max-h-[78vh]", "translate-y-[calc(50%-50vh+1rem)]", "scale-100")
            classes(expandPositionTransition)
        }
    }) {
        SessionFinishedOverlayHeader(
            collapsed = collapsed,
            state = state,
            onExpandCollapse = { collapsed = !collapsed },
        )
        SessionFinishedOverlayBody(collapsed, session, state)
    }
}

@Composable
private fun SessionFinishedOverlayHeader(
    collapsed: Boolean,
    state: SessionState.Finished,
    onExpandCollapse: () -> Unit,
) {
    val winner = state.result.winner

    Button({
        classes(springTransition)
        classes(
            "group", "relative", "flex", "w-full", "items-center", "justify-between", "gap-4", "transition-all",
            "cursor-pointer", "select-none", "border-0", "bg-transparent", "text-slate-200", "hover:text-slate-50",
        )
        attr("aria-expanded", (!collapsed).toString())
        attr("aria-label", if (collapsed) "Expand finished session overlay" else "Collapse finished session overlay")
        onClick { onExpandCollapse() }

        if (collapsed) {
            classes("px-4", "py-3")
        } else {
            classes("px-4", "pb-0", "pt-4", "sm:px-5", "sm:pt-5")
        }
    }) {
        Div({ classes("min-w-0", "flex", "items-center", "gap-3", "text-left") }) {
            Div({
                classes(
                    "grid", "size-10", "shrink-0", "place-items-center", "rounded-full", "border",
                    "font-black", "shadow-lg",
                )
                classes(if (winner == null) "border-amber-300/40" else "border-emerald-300/40")
                classes(if (winner == null) "bg-amber-300/15" else "bg-emerald-400/15")
                classes(if (winner == null) "text-amber-200" else "text-emerald-200")
            }) {
                Text(if (winner == null) "=" else "1")
            }

            Div({ classes("min-w-0") }) {
                Div({ classes("flex", "min-w-0", "items-center", "gap-2") }) {
                    Span({ classes("truncate", "text-lg", "font-extrabold", "text-slate-100", "sm:text-xl") }) {
                        Text(winner?.let { "${it.displayName} wins" } ?: "Draw")
                    }
                    if (!collapsed) {
                        Badge(if (winner == null) Color.Yellow else Color.Emerald, { classes("hidden", "shrink-0", "sm:inline-flex") }) {
                            Text(state.result.reason.label)
                        }
                    }
                }
                Span({ classes("block", "truncate", "text-xs", "font-semibold", "uppercase", "text-slate-500") }) {
                    Text("Match finished")
                }
            }
        }

        ChevronDownIcon {
            classes(
                "mr-0.5", "size-5", "shrink-0", "text-slate-400",
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
            classes("grid-rows-[1fr]", "opacity-100", "px-4", "pb-4", "pt-4", "sm:px-5", "sm:pb-5")
        }
    }) {
        Div({ classes("min-h-0", "overflow-y-auto", "pr-1") }) {
            Div({ classes("grid", "gap-4") }) {
                SessionFinishedResultSummary(session, state)

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
private fun SessionFinishedResultSummary(session: LiveSession, state: SessionState.Finished) {
    val winner = state.result.winner

    Div({
        classes(
            "relative", "overflow-hidden", "rounded-2xl", "border", "px-4", "py-4", "text-center",
            "bg-slate-900/75",
        )
        classes(if (winner == null) "border-amber-300/30" else "border-emerald-300/30")
    }) {
        Div({ classes("relative", "flex", "flex-wrap", "items-center", "justify-center", "gap-2") }) {
            Badge(if (winner == null) Color.Yellow else Color.Emerald) {
                Text(state.result.reason.label)
            }
            Badge(Color.Neutral) {
                Text(state.result.duration.formatCompact())
            }
            Badge(Color.Neutral) {
                Text("${session.game.moveCount} moves")
            }
        }
        P({
            classes("relative", "mt-3", "text-3xl", "font-black", "text-slate-50", "sm:text-4xl")
        }) {
            Text(winner?.let { "${it.displayName} wins" } ?: "Draw")
        }
        P({ classes("relative", "mt-1", "text-sm", "font-medium", "text-slate-400") }) {
            Text(if (winner == null) "Both players split the result." else "Final result is recorded.")
        }
    }
}

@Composable
private fun SessionFinishedPlayerCard(player: LiveSessionPlayer, session: LiveSession, state: SessionState.Finished) {
    val winner = state.result.winner

    SubCard({
        classes("relative", "overflow-hidden", "p-4", "transition", "hover:-translate-y-0.5")
        when (winner) {
            null -> classes("bg-amber-300/10!", "border-amber-300/35!")
            player -> classes("bg-emerald-500/14!", "border-emerald-400/45!", "shadow-emerald-950/30")
            else -> classes("bg-rose-400/12!", "border-rose-400/35!")
        }
    }) {
        SessionFinishedPlayerHeader(player, winner)

        Div({ classes("mt-4", "flex", "flex-wrap", "items-center", "gap-2") }) {
            SessionFinishedEloBadge(player, winner, session.gameOptions.rated)
            if (player in state.rematchAcceptedPlayers) {
                Badge(Color.Sky) {
                    Text("Rematch accepted")
                }
            }
        }
    }
}

@Composable
private fun SessionFinishedPlayerHeader(player: LiveSessionPlayer, winner: Player?) {
    Div({ classes("flex", "items-start", "justify-between", "gap-3") }) {
        Div({ classes("min-w-0", "flex", "items-center", "gap-3") }) {
            val theme by rememberTheme()
            Div({
                classes(
                    "grid", "size-11", "shrink-0", "place-items-center", "rounded-full", "border",
                    "bg-slate-950/60", "text-sm", "font-black", "text-slate-100", "shadow-md",
                )
                style { backgroundColor(theme.playerCssColor(player.color)) }
            }) {
                Text(player.displayName.take(1).uppercase())
            }

            Div({ classes("min-w-0") }) {
                H2({ classes("truncate", "text-lg", "font-bold", "text-slate-100") }) {
                    Text(player.displayName)
                }
                P({ classes("mt-0.5", "text-sm", "font-medium", "text-slate-400") }) {
                    Text("${player.elo} Elo")
                }
            }
        }

        Badge(player.outcomeColor(winner), { classes("shrink-0") }) {
            Text(player.outcomeLabel(winner))
        }
    }
}

@Composable
private fun SessionFinishedEloBadge(player: LiveSessionPlayer, winner: Player?, rated: Boolean) {
    val eloAdjustment = player.ratingAdjustment.takeIf { rated } ?: return

    Badge(Color.Neutral) {
        Text("Elo ")
        Span({ classes(player.eloAdjustmentColor(winner), "font-semibold") }) {
            Text(player.eloAdjustmentLabel(winner, eloAdjustment))
        }
    }
}

@Composable
private fun SessionFinishedTournamentSummary(session: LiveSession, state: SessionState.Finished) {
    val tournament = session.tournament ?: return
    if (tournament.matchInfo.bestOf < 3) return

    Div({
        classes("rounded-2xl", "border", "border-slate-700/70", "bg-slate-900/70", "px-4", "py-4")
    }) {
        Div({ classes("mb-4", "flex", "items-start", "justify-between", "gap-3") }) {
            Div({ classes("min-w-0") }) {
                Span({ classes("text-xs", "font-semibold", "uppercase", "text-slate-500") }) {
                    Text("Tournament")
                }
                P({ classes("mt-1", "truncate", "font-semibold", "text-slate-100") }) {
                    Text(tournament.tournamentInfo.name)
                }
            }
            Badge(Color.Neutral, { classes("shrink-0") }) {
                Text("Game ${tournament.matchInfo.currentGameNumber} of ${tournament.matchInfo.bestOf}")
            }
        }

        Div({ classes("grid", "gap-3") }) {
            session.players.forEach { player ->
                SessionFinishedTournamentPlayer(player, state.result.winner, tournament.matchInfo.requiredWins)
            }
        }
    }
}

@Composable
private fun SessionFinishedTournamentPlayer(
    player: LiveSessionPlayer,
    winner: Player?,
    requiredWins: Int,
) {
    val matchWins = player.finishedTournamentWins(winner)
    val progress = (matchWins * 100 / requiredWins).coerceIn(0, 100)

    Div({
        classes(
            "grid", "grid-cols-[minmax(0,1fr)_auto]", "items-center", "gap-3",
            "rounded-xl", "border", "border-slate-800", "bg-slate-950/35", "p-3",
        )
    }) {
        Div({ classes("min-w-0") }) {
            SessionFinishedTournamentPlayerLabel(player)
            Div({ classes("h-2", "overflow-hidden", "rounded-full", "bg-slate-950/80", "ring-1", "ring-white/5") }) {
                Div({
                    classes("h-full", "rounded-full", "transition-all", "duration-500")
                    classes(if (matchWins > 0) "bg-emerald-400" else "bg-slate-700")
                    style { width(progress.percent) }
                })
            }
        }
        SessionFinishedTournamentScore(matchWins, requiredWins)
    }
}

@Composable
private fun SessionFinishedTournamentPlayerLabel(player: LiveSessionPlayer) {
    Div({ classes("mb-2", "flex", "items-center", "gap-2") }) {
        val theme by rememberTheme()
        Div({
            classes("rounded-full", "size-2.5", "shrink-0")
            style { backgroundColor(theme.playerCssColor(player.color)) }
        })
        Span({ classes("truncate", "text-sm", "font-semibold", "text-slate-200") }) {
            Text(player.displayName)
        }
    }
}

@Composable
private fun SessionFinishedTournamentScore(matchWins: Int, requiredWins: Int) {
    Span({ classes("inline-flex", "w-14", "items-baseline", "justify-end", "font-semibold", "tabular-nums") }) {
        Span({
            classes("text-xl")
            classes(if (matchWins == requiredWins) "text-emerald-300" else "text-slate-200")
        }) {
            Text(matchWins.toString())
        }
        Span({ classes("text-sm", "text-slate-500") }) {
            Text("/$requiredWins")
        }
    }
}

private fun LiveSessionPlayer.outcomeColor(winner: Player?) = when (winner) {
    null -> Color.Yellow
    this -> Color.Emerald
    else -> Color.Rose
}

private fun LiveSessionPlayer.outcomeLabel(winner: Player?) = when (winner) {
    null -> "Draw"
    this -> "Winner"
    else -> "Lost"
}

private fun LiveSessionPlayer.eloAdjustmentColor(winner: Player?) = when (winner) {
    this -> "text-emerald-300"
    null -> "text-slate-300"
    else -> "text-rose-300"
}

private fun LiveSessionPlayer.eloAdjustmentLabel(
    winner: Player?,
    ratingAdjustment: RatingAdjustment,
) = when (winner) {
    this -> "+${ratingAdjustment.eloGain}"
    null -> "0"
    else -> "${ratingAdjustment.eloLoss}"
}

private fun LiveSessionPlayer.finishedTournamentWins(winner: Player?) =
    tournamentMatchWins?.let { if (this == winner) it + 1 else it } ?: 0

private val GameFinishReason.label get() = when (this) {
    is GameFinishReason.Regular -> "Six in a Row"
    is GameFinishReason.Timeout -> "Timeout"
    is GameFinishReason.Surrender -> "Surrender"
    is GameFinishReason.Disconnect -> "Disconnect"
    is GameFinishReason.DrawAgreement -> "Draw agreed"
    is GameFinishReason.Terminated -> "Terminated"
}
