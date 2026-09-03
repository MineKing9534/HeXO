package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import de.mineking.hexo.game.model.game.GameVisibility
import de.mineking.hexo.game.model.session.LobbySession
import de.mineking.hexo.game.model.session.SessionPlayer
import de.mineking.hexo.game.model.session.SessionPlayerConnectionStatus
import de.mineking.hexo.web.components.BackLink
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.BadgeSize
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.ContentCard
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.components.SubCardVariant
import de.mineking.hexo.web.format
import de.mineking.hexo.web.icons.EyeIcon
import de.mineking.hexo.web.icons.EyeOffIcon
import de.mineking.hexo.web.icons.TimeControlIcon
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.pages.games.GameTypeBadge
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun LobbyOverlay(session: LobbySession) {
    ContentCard({
        classes(
            "my-auto", "flex", "max-w-4xl", "flex-col", "gap-5", "border-sky-900/70!", "bg-slate-900/95!",
            "bg-linear-to-br!", "from-slate-900!", "via-slate-900!", "to-sky-950/50!",
            "p-5", "shadow-[0_28px_80px_-24px_rgba(0,0,0,0.8)]!",
            "ring-1", "ring-white/5", "backdrop-blur-md", "sm:p-6",
        )
    }) {
        LobbyHeader(session)
        StandardLobby(session)
    }
}

@Composable
private fun LobbyHeader(session: LobbySession) {
    Div({ classes("flex", "flex-wrap", "items-start", "justify-between", "gap-4") }) {
        Div({ classes("min-w-0") }) {
            Div({ classes("mb-2", "flex", "items-center", "gap-2") }) {
                Span({ classes("size-2", "animate-pulse", "rounded-full", "bg-emerald-300", "shadow-sm", "shadow-emerald-300/50") })
                Span({ classes("text-xs", "font-bold", "tracking-widest", "text-emerald-300", "uppercase") }) {
                    Text("Lobby open")
                }
            }
            H1({ classes("text-2xl", "font-extrabold", "text-slate-100", "sm:text-3xl") }) {
                Text(if (session.tournament == null) "Waiting for an opponent" else "Preparing tournament game")
            }
            P({ classes("mt-1.5", "max-w-xl", "text-sm", "leading-relaxed", "text-slate-500") }) {
                Text("The game will start automatically as soon as every player is connected.")
            }
        }
        Div({ classes("shrink-0") }) {
            BackLink(AppRoute.SessionList, "All lobbies", compact = true)
        }
    }
}

@Composable
private fun StandardLobby(session: LobbySession) {
    Div({ classes("grid", "gap-4") }) {
        SubCard({ classes("overflow-hidden") }, SubCardVariant.Inset) {
            Div({ classes("p-4", "sm:p-5") }) {
                SessionSectionLabel("Players")
                Div({
                    classes(
                        "mt-3", "grid", "items-center", "gap-2",
                        "sm:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]", "sm:gap-3",
                    )
                }) {
                    LobbySeat(session.players.getOrNull(0))
                    Span({ classes("text-xs", "font-bold", "text-slate-600", "uppercase") }) { Text("vs") }
                    LobbySeat(session.players.getOrNull(1))
                }
            }
            LobbySettings(session)
        }
        TournamentSummary(session.tournament, session.players)
    }
}

@Composable
private fun LobbySeat(player: SessionPlayer?) {
    Div({
        classes(
            "flex", "min-w-0", "flex-col", "items-center", "gap-2", "rounded-xl", "border", "p-3", "text-center",
        )
        when (player?.connectionStatus) {
            null -> classes("border-dashed", "border-slate-700", "bg-slate-900/55")
            SessionPlayerConnectionStatus.Connected -> classes("border-emerald-400/25", "bg-emerald-500/10")
            SessionPlayerConnectionStatus.Orphaned -> classes("border-amber-400/25", "bg-amber-500/10")
            SessionPlayerConnectionStatus.Disconnected -> classes("border-slate-700", "bg-slate-900/55")
        }
    }) {
        SessionPlayerIcon(player)
        Div({ classes("min-w-0", "w-full") }) {
            P({ classes("truncate", "text-sm", "font-semibold", if (player == null) "text-slate-500" else "text-slate-100") }) {
                Text(player?.displayName ?: "Open seat")
            }
            if (player == null) {
                P({ classes("mt-0.5", "text-xs", "text-slate-600") }) { Text("Waiting to join") }
            } else {
                SessionPlayerMeta(player, disconnectedAsWaiting = true)
            }
        }
    }
}

@Composable
private fun LobbySettings(session: LobbySession) {
    Div({
        classes(
            "grid", "border-t", "border-slate-800", "bg-slate-950/25",
            "divide-y", "divide-slate-700/70", "sm:grid-cols-3", "sm:divide-x", "sm:divide-y-0",
        )
    }) {
        LobbyOption("Mode") {
            GameTypeBadge(session.gameOptions, session.tournament, BadgeSize.Large)
        }
        LobbyOption("Time control") {
            Badge(Color.Neutral, size = BadgeSize.Large) {
                TimeControlIcon { classes("size-4", "fill-none", "stroke-current") }
                Text(session.gameOptions.timeControl.format())
            }
        }
        LobbyOption("Visibility") {
            Badge(
                if (session.gameOptions.visibility == GameVisibility.Public) Color.Sky else Color.Yellow,
                size = BadgeSize.Large,
            ) {
                when (session.gameOptions.visibility) {
                    GameVisibility.Public -> EyeIcon { classes("size-4") }
                    GameVisibility.Private -> EyeOffIcon { classes("size-4") }
                }
                Text(session.gameOptions.visibility.name)
            }
        }
    }
}

@Composable
private fun LobbyOption(label: String, content: @Composable () -> Unit) {
    SessionMetric(label, content = content)
}
