package de.mineking.hexo.web.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.varabyte.kobweb.compose.dom.svg.Circle
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.navigation.Anchor
import de.mineking.hexo.hds.session.LobbySession
import de.mineking.hexo.hds.session.SessionPlayer
import de.mineking.hexo.hds.session.SessionRepository
import de.mineking.hexo.hds.session.hasStarted
import de.mineking.hexo.hds.utils.TimeControl
import de.mineking.hexo.web.components.AppLayout
import de.mineking.hexo.web.components.AppPage
import de.mineking.hexo.web.rememberHdsApiClient
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg

@Page
@Composable
fun Index() {
    val client = rememberHdsApiClient(withSocket = true)

    AppLayout(activePage = AppPage.Home) {
        Div({ classes("lg:h-12") })

        if (client == null) {
            LoadingState()
        } else {
            LobbyList(client.sessionRepository)
        }
    }
}

@Composable
private fun LoadingState() {
    Div({
        classes(
            "grid", "min-h-64", "place-items-center", "rounded-xl", "border", "border-slate-800",
            "bg-slate-800/40", "p-6",
        )
    }) {
        Text("Connecting to lobby service...")
    }
}

@Composable
private fun LobbyList(sessionRepository: SessionRepository) {
    val lobbies by sessionRepository.lobbies.collectAsState()
    val sortedLobbies = lobbies.values.sortedWith(
        compareByDescending<LobbySession> { it.hasStarted() }
            .thenByDescending { it.createdAt },
    )

    Div({
        classes(
            "flex", "flex-col", "gap-4", "rounded-2xl", "border", "border-slate-800", "bg-slate-800/40",
            "p-4", "shadow-xl", "shadow-black/10",
        )
    }) {
        Div({ classes("flex", "items-center", "justify-between", "gap-3") }) {
            H2({ classes("text-lg", "font-bold", "text-slate-100") }) {
                Text("Lobbies")
            }
            Span({ classes("text-sm", "text-slate-500") }) {
                Span({ classes("font-semibold") }) {
                    Text("${sortedLobbies.size} ")
                }
                Span {
                    Text(if (sortedLobbies.size == 1) "lobby" else "lobbies")
                }
            }
        }

        if (sortedLobbies.isEmpty()) {
            EmptyLobbyState()
        } else {
            Div({ classes("grid", "gap-4") }) {
                sortedLobbies.forEach { lobby ->
                    LobbyCard(lobby)
                }
            }
        }
    }
}

@Composable
private fun EmptyLobbyState() {
    Div({
        classes(
            "grid", "min-h-64", "place-items-center", "rounded-xl", "bg-slate-950/40", "p-6", "text-center",
            "border", "border-slate-600/50", "border-dashed",
        )
    }) {
        Div({ classes("flex", "flex-col", "items-center", "gap-2") }) {
            H2({ classes("text-base", "font-semibold", "text-slate-200") }) {
                Text("No open lobbies")
            }
            P({ classes("max-w-md", "text-sm", "leading-relaxed", "text-slate-500") }) {
                Text("There are no public lobbies right now. The sandbox is still available for position analysis.")
            }
        }
    }
}

@Composable
private fun LobbyCard(lobby: LobbySession) {
    Anchor("/sessions/${lobby.id.value}", {
        classes(
            "group", "grid", "gap-4", "rounded-xl", "border", "border-slate-600/40", "bg-slate-700/40", "p-3",
            "shadow-md", "shadow-slate-700/10", "transition", "hover:border-slate-600/80", "hover:bg-slate-700/80",
            "md:grid-cols-[1fr_auto]", "md:items-center",
        )
    }) {
        Div({ classes("flex", "min-w-0", "flex-col", "gap-3") }) {
            Div({ classes("flex", "flex-wrap", "items-center", "gap-2") }) {
                StatusBadge(lobby)
                GameTypeBadge(lobby.gameOptions.rated)
                TimeControlBadge(lobby.gameOptions.timeControl)
            }

            Div({ classes("min-w-0") }) {
                H2({ classes("truncate", "text-xl", "text-base", "font-semibold", "text-slate-100") }) {
                    Text(lobby.players.formatPlayers())
                }
                P({ classes("mt-1", "truncate", "text-xs", "font-mono", "text-slate-500") }) {
                    Text(lobby.id.value)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(lobby: LobbySession) {
    val status = if (lobby.hasStarted()) "In game" else "Waiting"

    Span({
        classes("rounded-full", "border", "px-2.5", "py-1", "text-xs", "font-semibold")
        if (lobby.hasStarted()) {
            classes("border-sky-500/40", "bg-sky-500/15", "text-sky-300")
        } else {
            classes("border-emerald-500/40", "bg-emerald-500/15", "text-emerald-300")
        }
    }) {
        Text(status)
    }
}

@Composable
private fun GameTypeBadge(rated: Boolean) {
    Span({
        classes("inline-flex", "items-center", "gap-1.5", "rounded-full", "border", "px-2.5", "py-1", "text-xs", "font-medium")
        if (rated) {
            classes("border-yellow-400/40", "bg-yellow-400/15", "text-yellow-200")
        } else {
            classes("border-slate-700", "bg-slate-950/60", "text-slate-300")
        }
    }) {
        GameTypeIcon(rated)
        Text(if (rated) "Rated" else "Casual")
    }
}

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
private fun GameTypeIcon(rated: Boolean) {
    if (rated) {
        Svg("0 0 16 16", {
            attr("aria-hidden", "true")
            attr("fill", "currentColor")
            classes("h-3.5", "w-3.5", "fill-current")
        }) {
            Path("M8 1.9l1.7 3.46 3.82.56-2.76 2.69.65 3.8L8 10.59 4.6 12.4l.65-3.8L2.5 5.92l3.8-.56L8 1.9Z")
        }
    } else {
        Svg("0 0 16 16", {
            attr("aria-hidden", "true")
            attr("fill", "none")
            attr("stroke", "currentColor")
            classes("h-3.5", "w-3.5", "fill-none", "stroke-current")
        }) {
            Circle(attrs = {
                cx(8)
                cy(8)
                r(4.75)
                attr("stroke-width", "1.5")
            })
            Path("M5 8h6", attrs = {
                attr("stroke-width", "1.5")
                attr("stroke-linecap", "round")
            })
        }
    }
}

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
private fun TimeControlBadge(timeControl: TimeControl) {
    Span({
        classes(
            "inline-flex", "items-center", "gap-1.5", "rounded-full", "border", "border-slate-700",
            "bg-slate-950/60", "px-2.5", "py-1", "text-xs", "font-medium", "text-slate-300",
        )
    }) {
        Svg("0 0 16 16", {
            attr("aria-hidden", "true")
            attr("fill", "none")
            attr("stroke", "currentColor")
            classes("h-4", "w-4", "fill-none", "stroke-current")
        }) {
            Circle(attrs = {
                cx(8)
                cy(8)
                r(5.25)
                attr("stroke-width", "1.5")
            })
            Path("M8 5.2v3.2l2.1 1.25", attrs = {
                attr("stroke-width", "1.5")
                attr("stroke-linecap", "round")
                attr("stroke-linejoin", "round")
            })
        }
        Text(timeControl.format())
    }
}

private fun List<SessionPlayer>.formatPlayers() = when (size) {
    1 -> first().displayName
    else -> joinToString(" vs ") { it.displayName }
}

private fun TimeControl.format() = when (this) {
    TimeControl.Unlimited -> "Unlimited"
    is TimeControl.Turn -> "Turn $turnTime"
    is TimeControl.Match -> "Match $mainTime +$increment"
}
