package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.game.model.game.FinishedGamePlayer
import de.mineking.hexo.game.model.game.GameFinishReason
import de.mineking.hexo.game.model.game.GameResult
import de.mineking.hexo.game.model.game.GameWithPosition
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.game.isGuest
import de.mineking.hexo.game.model.session.LiveSession
import de.mineking.hexo.game.model.session.LiveSessionPlayer
import de.mineking.hexo.game.model.session.RatingAdjustment
import de.mineking.hexo.game.model.session.SessionState
import de.mineking.hexo.web.board.PlayerName
import de.mineking.hexo.web.board.gamePlayer
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.Card
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.components.SubCardVariant
import de.mineking.hexo.web.formatCompact
import de.mineking.hexo.web.icons.CheckIcon
import de.mineking.hexo.web.icons.ChevronDownIcon
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
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
fun SessionFinishedOverlay(session: LiveSession, state: SessionState.Detailed.Finished) {
    FinishedGameOverlay(
        game = session.game,
        result = state.result,
        rematchAcceptedPlayers = state.rematchAcceptedPlayers,
    )
}

@Composable
fun FinishedGameOverlay(
    game: GameWithPosition,
    result: GameResult = requireNotNull(game.result),
    rematchAcceptedPlayers: List<Player> = emptyList(),
) {
    var collapsed by remember(game.id, result) { mutableStateOf(false) }

    Card({
        classes(
            "fixed", "bottom-2", "left-1/2", "z-50", "w-[calc(100%-1rem)]", "-translate-x-1/2", "overflow-hidden",
            "transition-all", "sm:bottom-5", "sm:w-[calc(100%-3rem)]",
            "bg-slate-950/94!", "border-slate-700/80!", "backdrop-blur-md", "border",
            "shadow-2xl", "shadow-black/45", "ring-1", "ring-white/5",
        )

        if (collapsed) {
            classes("max-w-xl", "translate-y-0", "scale-100")
            classes(collapsePositionTransition)
        } else {
            classes("max-w-4xl", "max-h-[82vh]", "translate-y-[calc(50%-50vh+1rem)]", "scale-100")
            classes(expandPositionTransition)
        }
    }) {
        SessionFinishedOverlayHeader(
            collapsed = collapsed,
            result = result,
            onExpandCollapse = { collapsed = !collapsed },
        )
        SessionFinishedOverlayBody(collapsed, game, result, rematchAcceptedPlayers)
    }
}

@Composable
private fun SessionFinishedOverlayHeader(
    collapsed: Boolean,
    result: GameResult,
    onExpandCollapse: () -> Unit,
) {
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
            classes("px-4", "py-2.5")
        } else {
            classes("px-4", "pb-0", "pt-4", "sm:px-5", "sm:pt-5")
        }
    }) {
        if (collapsed) CollapsedFinishedTitle(result) else ExpandedFinishedTitle(result)

        ChevronDownIcon {
            classes(
                "size-5", "shrink-0", "rounded-md", "p-1", "box-content", "text-slate-400",
                "transition-all", "group-hover:text-slate-100",
            )
            classes(springTransition)
            if (collapsed) {
                classes("mr-0.5", "self-center", "rotate-180")
            } else {
                classes("absolute", "right-4", "top-4", "sm:right-5", "sm:top-5")
            }
        }
    }
}

@Composable
private fun CollapsedFinishedTitle(result: GameResult) {
    val winner = result.winner

    Div({ classes("min-w-0", "flex", "items-center", "gap-3", "text-left") }) {
        Div({
            classes("grid", "size-9", "shrink-0", "place-items-center", "rounded-lg", "border")
            if (winner == null) {
                classes("border-amber-300/40", "bg-amber-300/15", "text-amber-200")
            } else {
                classes("border-emerald-300/40", "bg-emerald-400/15", "text-emerald-200")
            }
        }) {
            if (winner == null) Text("=") else CheckIcon { classes("size-4") }
        }
        Div({ classes("min-w-0") }) {
            Span({ classes("block", "truncate", "font-bold", "text-slate-100") }) {
                Text(winner?.let { "${it.displayName} wins" } ?: "Game drawn")
            }
            Span({ classes("block", "truncate", "text-xs", "text-slate-500") }) {
                Text(result.reason.label)
            }
        }
    }
}

@Composable
private fun ExpandedFinishedTitle(result: GameResult) {
    val winner = result.winner

    Div({ classes("min-w-0", "pr-10", "text-left") }) {
        Div({ classes("mb-2", "flex", "items-center", "gap-2") }) {
            Span({ classes("size-2", "rounded-full", if (winner == null) "bg-amber-300" else "bg-emerald-300") })
            Span({
                classes(
                    "text-xs", "font-bold", "tracking-widest",
                    if (winner == null) "text-amber-300" else "text-emerald-300", "uppercase",
                )
            }) {
                Text("Match complete")
            }
        }
        Span({ classes("block", "truncate", "text-2xl", "font-extrabold", "text-slate-100", "sm:text-3xl") }) {
            Text(winner?.let { "${it.displayName} wins" } ?: "Game drawn")
        }
        Span({ classes("mt-1.5", "block", "text-sm", "font-medium", "text-slate-500") }) {
            Text("${result.reason.label} · ${result.duration.formatCompact()}")
        }
    }
}

@Composable
private fun SessionFinishedOverlayBody(
    collapsed: Boolean,
    game: GameWithPosition,
    result: GameResult,
    rematchAcceptedPlayers: List<Player>,
) {
    Div({
        classes("grid", "transition-all")
        classes(springTransition)

        if (collapsed) {
            classes("grid-rows-[0fr]", "opacity-0")
        } else {
            classes("grid-rows-[1fr]", "opacity-100", "px-4", "pb-4", "pt-4", "sm:px-5", "sm:pb-5")
        }
    }) {
        Div({ classes("min-h-0", "overflow-y-auto", "pr-1") }) {
            Div({ classes("grid", "gap-4") }) {
                SubCard({ classes("overflow-hidden") }) {
                    Div({ classes("p-4", "sm:p-5") }) {
                        SessionSectionLabel("Final standings")
                        Div({ classes("mt-3") }) {
                            SessionFinishedMatchup(game, result, rematchAcceptedPlayers)
                        }
                    }
                    SessionFinishedResultSummary(game, result)
                }

                TournamentSummary(game.tournament, game.players) { player ->
                    player.tournamentMatchWins?.let { if (player == result.winner) it + 1 else it } ?: 0
                }
            }
        }
    }
}

@Composable
private fun SessionFinishedMatchup(
    game: GameWithPosition,
    result: GameResult,
    rematchAcceptedPlayers: List<Player>,
) {
    Div({ classes("grid", "items-stretch", "gap-2", "sm:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]", "sm:gap-3") }) {
        game.players.forEachIndexed { index, player ->
            if (index > 0) {
                Div({ classes("flex", "items-center", "justify-center") }) {
                    Span({ classes("text-xs", "font-bold", "text-slate-600", "uppercase") }) { Text("vs") }
                }
            }
            SessionFinishedPlayerCard(player, game, result, rematchAcceptedPlayers)
        }
    }
}

@Composable
private fun SessionFinishedResultSummary(game: GameWithPosition, result: GameResult) {
    val winner = result.winner

    Div({
        classes(
            "grid", "border-t", "border-slate-800", "bg-slate-950/25",
            "divide-y", "divide-slate-700/70", "sm:grid-cols-3", "sm:divide-x", "sm:divide-y-0",
        )
    }) {
        SessionMetric("Result", if (winner == null) "text-amber-300" else "text-emerald-300") {
            Text(result.reason.label)
        }
        SessionMetric("Duration") { Text(result.duration.formatCompact()) }
        SessionMetric("Moves played") { Text(game.moveCount.toString()) }
    }
}

@Composable
private fun SessionFinishedPlayerCard(
    player: Player,
    game: GameWithPosition,
    result: GameResult,
    rematchAcceptedPlayers: List<Player>,
) {
    val winner = result.winner

    SubCard({
        classes(
            "relative", "h-full", "overflow-hidden", "p-3", "text-center", "transition",
        )
    }, if (winner == player) SubCardVariant.Highlighted else SubCardVariant.Deep) {
        SessionFinishedPlayerHeader(result, player, game.options.rated)

        if (player in rematchAcceptedPlayers) {
            Div({ classes("mt-2", "flex", "flex-wrap", "items-center", "justify-center", "gap-1.5") }) {
                Badge(Color.Sky) {
                    Text("Rematch accepted")
                }
            }
        }
    }
}

@Composable
private fun SessionFinishedPlayerHeader(result: GameResult, player: Player, rated: Boolean) {
    val eloAdjustment = if (rated) player.eloAdjustment(result.winner) else null

    Div({ classes("flex", "min-w-0", "flex-col", "items-center", "gap-1.5") }) {
        SessionPlayerIcon(player)

        Div({ classes("w-full", "min-w-0") }) {
            PlayerName(player.gamePlayer, attrs = {
                classes("font-semibold")
                if (player == result.winner) classes("text-emerald-200!")
            })
            if (player is LiveSessionPlayer) {
                SessionPlayerMeta(player, eloAdjustment = eloAdjustment)
            } else if (!player.isGuest()) {
                Div({ classes("mt-0.5", "text-xs") }) { PlayerElo(player, eloAdjustment) }
            }
        }
    }
}

private fun Player.eloAdjustment(winner: Player?) = when (this) {
    is FinishedGamePlayer -> eloChange
    is LiveSessionPlayer -> ratingAdjustment?.let { eloAdjustment(winner, it) }
    else -> null
}

private fun LiveSessionPlayer.eloAdjustment(
    winner: Player?,
    ratingAdjustment: RatingAdjustment,
) = when (winner) {
    this -> ratingAdjustment.eloGain
    null -> 0
    else -> ratingAdjustment.eloLoss
}

private val GameFinishReason.label get() = when (this) {
    is GameFinishReason.Regular -> "$length in a Row"
    is GameFinishReason.Timeout -> "Timeout"
    is GameFinishReason.Surrender -> "Surrender"
    is GameFinishReason.Disconnect -> "Disconnect"
    is GameFinishReason.DrawAgreement -> "Draw agreed"
    is GameFinishReason.Terminated -> "Terminated"
}
