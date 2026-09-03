package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import de.mineking.hexo.game.model.game.TournamentMatchSnapshot
import de.mineking.hexo.game.model.session.SessionPlayer
import de.mineking.hexo.game.model.tournament.requiredWins
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.SubCard
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
internal fun TournamentSummary(
    tournament: TournamentMatchSnapshot?,
    players: List<SessionPlayer>,
    wins: (SessionPlayer) -> Int = { it.tournamentMatchWins ?: 0 },
) {
    tournament ?: return

    SubCard({ classes("overflow-hidden") }) {
        TournamentHeader(tournament)
        Div({
            classes(
                "grid", "gap-2", "border-t", "p-4", "sm:grid-cols-2", "sm:px-5", "border-slate-800",
            )
        }) {
            players.forEach { player ->
                TournamentPlayerStanding(player, wins(player), tournament.matchInfo.requiredWins)
            }
        }
    }
}

@Composable
private fun TournamentHeader(tournament: TournamentMatchSnapshot) {
    Div({
        classes("flex", "items-start", "justify-between", "gap-3", "p-4", "sm:px-5", "bg-slate-950/25")
    }) {
        Div({ classes("min-w-0") }) {
            SessionSectionLabel("Tournament")
            P({ classes("mt-1", "truncate", "font-semibold", "text-slate-100") }) {
                Text(tournament.tournament.info.name)
            }
        }
        Badge(Color.Neutral, { classes("shrink-0") }) {
            Text("Game ${tournament.matchInfo.currentGameNumber} of ${tournament.matchInfo.bestOf}")
        }
    }
}

@Composable
private fun TournamentPlayerStanding(player: SessionPlayer, wins: Int, requiredWins: Int) {
    val progress = (wins * 100 / requiredWins).coerceIn(0, 100)

    Div({ classes("rounded-lg", "bg-slate-950/35", "px-3", "py-2.5") }) {
        Div({ classes("flex", "items-start", "justify-between", "gap-3") }) {
            Div({ classes("min-w-0") }) {
                Span({ classes("block", "truncate", "text-sm", "font-semibold", "text-slate-300") }) {
                    Text(player.displayName)
                }
            }
            TournamentScore(wins, requiredWins)
        }
        Div({ classes("mt-2", "h-1.5", "overflow-hidden", "rounded-full", "bg-slate-800") }) {
            Div({
                classes("h-full", "rounded-full", if (wins > 0) "bg-emerald-400" else "bg-slate-700")
                style { width(progress.percent) }
            })
        }
    }
}

@Composable
private fun TournamentScore(wins: Int, requiredWins: Int) {
    Span({ classes("shrink-0", "font-semibold", "tabular-nums") }) {
        Span({ classes("text-lg", if (wins > 0) "text-emerald-300" else "text-slate-300") }) {
            Text(wins.toString())
        }
        Span({ classes("text-sm", "text-slate-500") }) { Text("/$requiredWins") }
    }
}
