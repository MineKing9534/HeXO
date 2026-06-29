package de.mineking.hexo.web.session

import androidx.compose.runtime.Composable
import de.mineking.hexo.hds.session.LobbySession
import de.mineking.hexo.hds.session.SessionPlayer
import de.mineking.hexo.hds.utils.TimeControl
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.ContentCard
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.pages.sessions.BackToLobbiesLink
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun LobbyOverlay(session: LobbySession) {
    ContentCard({
        classes("flex", "flex-col", "gap-5", "p-6")
    }) {
        Div({ classes("flex", "flex-col", "items-center", "gap-3", "text-center") }) {
            H1({ classes("text-2xl", "font-extrabold", "text-slate-100") }) {
                Text("Waiting for opponent")
            }
            LoadingIndicator { classes("size-9") }

            P({ classes("mt-4", "max-w-xl", "text-sm", "leading-relaxed", "text-slate-400") }) {
                Text("The lobby is open. This page will switch to the board automatically once another player joins.")
            }
        }

        Div({ classes("grid", "gap-4", "md:grid-cols-2") }) {
            LobbyPlayerCard(session.players.singleOrNull())
            LobbyDetailsCard(session)
        }

        Div({ classes("flex", "justify-center") }) {
            BackToLobbiesLink()
        }
    }
}

@Composable
private fun LobbyPlayerCard(player: SessionPlayer?) {
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

private fun TimeControl.format() = when (this) {
    TimeControl.Unlimited -> "Unlimited"
    is TimeControl.Turn -> "Turn $turnTime"
    is TimeControl.Match -> "Match $mainTime +$increment"
}
