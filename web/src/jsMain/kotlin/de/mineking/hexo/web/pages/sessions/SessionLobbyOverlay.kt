package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.session.LobbySession
import de.mineking.hexo.game.model.tournament.requiredWins
import de.mineking.hexo.web.components.BackLink
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.ContentCard
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.format
import de.mineking.hexo.web.layout.AppRoute
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun LobbyOverlay(session: LobbySession) {
    ContentCard({
        classes("flex", "flex-col", "gap-5", "p-6", "my-auto")
    }) {
        Div({ classes("flex", "flex-col", "items-center", "gap-3", "text-center") }) {
            H1({ classes("text-2xl", "font-extrabold", "text-slate-100") }) {
                Text("Waiting for players to join...")
            }
            LoadingIndicator { classes("size-9") }
        }

        if (session.tournament == null) {
            Div({ classes("grid", "gap-4", "md:grid-cols-2") }) {
                LobbyPlayerCard(session.players.singleOrNull())
                LobbyDetailsCard(session)
            }
        } else {
            TournamentLobbyCard(session)
        }

        Div({ classes("flex", "justify-center") }) {
            BackLink(AppRoute.LobbyList, "Back to lobbies")
        }
    }
}

@Composable
private fun TournamentLobbyCard(session: LobbySession) {
    val tournament = session.tournament ?: return

    SubCard({ classes("p-4") }) {
        Div({ classes("flex", "flex-col", "gap-4") }) {
            Div({ classes("flex", "items-start", "justify-between", "gap-3") }) {
                Div({ classes("min-w-0") }) {
                    Span({ classes("text-xs", "font-semibold", "uppercase", "text-slate-500") }) {
                        Text("Tournament")
                    }
                    P({ classes("mt-1", "truncate", "font-semibold", "text-slate-100") }) {
                        Text(tournament.tournament.info.name)
                    }
                }
                Badge(Color.Neutral, { classes("shrink-0") }) {
                    Text("Game ${tournament.matchInfo.currentGameNumber} of ${tournament.matchInfo.bestOf}")
                }
            }

            Div({ classes("grid", "gap-3") }) {
                session.players.forEach { player ->
                    TournamentLobbyPlayer(player, tournament.matchInfo.requiredWins)
                }
            }
        }
    }
}

@Composable
private fun TournamentLobbyPlayer(player: Player, requiredWins: Int) {
    val connected = player.elo != -1
    val matchWins = player.tournamentMatchWins ?: 0

    Div({
        classes(
            "grid", "grid-cols-[minmax(0,1fr)_auto]", "items-center", "gap-3",
            "rounded-xl", "border", "border-slate-800", "bg-slate-950/30", "p-3",
        )
    }) {
        Div({ classes("min-w-0", "flex", "items-center", "gap-3") }) {
            Div({
                classes(
                    "grid", "size-10", "shrink-0", "place-items-center", "rounded-full",
                    "border", "text-sm", "font-bold",
                )
                if (connected) {
                    classes("border-emerald-500/40", "bg-emerald-500/15", "text-emerald-200")
                } else {
                    classes("border-slate-700", "bg-slate-800/60", "text-slate-400")
                }
            }) {
                Text(player.displayName.take(1).uppercase())
            }

            Div({ classes("min-w-0") }) {
                P({ classes("truncate", "font-semibold", "text-slate-100") }) {
                    Text(player.displayName)
                }
                Div({ classes("mt-1", "flex", "flex-wrap", "items-center", "gap-2") }) {
                    Badge(if (connected) Color.Emerald else Color.Neutral) {
                        Text(if (connected) "Connected" else "Waiting")
                    }
                }
            }
        }

        Span({ classes("shrink-0", "font-semibold", "tabular-nums") }) {
            Span({
                classes("text-lg")
                classes(if (matchWins == requiredWins - 1) "text-emerald-300" else "text-slate-300")
            }) {
                Text(matchWins.toString())
            }
            Span({ classes("text-sm", "text-slate-500") }) {
                Text("/$requiredWins")
            }
        }
    }
}

@Composable
private fun LobbyPlayerCard(player: Player?) {
    SubCard({ classes("p-4") }) {
        Div({ classes("flex", "flex-col", "gap-3") }) {
            Span({ classes("text-xs", "font-semibold", "uppercase", "text-slate-500") }) {
                Text("Joined player")
            }
            if (player == null) {
                P({ classes("text-sm", "text-slate-400") }) {
                    Text("Waiting for the first player to reconnect...")
                }
            } else {
                Div({ classes("flex", "items-center", "gap-3") }) {
                    Div({
                        classes(
                            "grid", "size-10", "shrink-0", "place-items-center", "rounded-full",
                            "border", "border-emerald-500/40", "bg-emerald-500/15",
                            "text-sm", "font-bold", "text-emerald-200",
                        )
                    }) {
                        Text(player.displayName.take(1).uppercase())
                    }
                    Div({ classes("min-w-0") }) {
                        P({ classes("truncate", "font-semibold", "text-slate-100") }) {
                            Text(player.displayName)
                        }
                        P({ classes("text-sm", "text-slate-500") }) {
                            Text("${player.elo} Elo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyDetailsCard(session: LobbySession) {
    SubCard({ classes("p-4") }) {
        Div({ classes("flex", "flex-col", "gap-3") }) {
            Span({ classes("text-xs", "font-semibold", "uppercase", "text-slate-500") }) {
                Text("Lobby")
            }
            Div({ classes("flex", "flex-wrap", "gap-2") }) {
                Badge(if (session.gameOptions.rated) Color.Yellow else Color.Neutral) {
                    Text(if (session.gameOptions.rated) "Rated" else "Casual")
                }
                Badge(Color.Neutral) {
                    Text(session.gameOptions.timeControl.format())
                }
            }
            LobbyDetail("Session ID", session.id.value)
        }
    }
}

@Composable
private fun LobbyDetail(label: String, value: String) {
    Div({ classes("grid", "gap-1") }) {
        Span({ classes("text-xs", "font-semibold", "uppercase", "text-slate-500") }) {
            Text(label)
        }
        Span({ classes("break-all", "font-mono", "text-sm", "text-slate-300") }) {
            Text(value)
        }
    }
}
