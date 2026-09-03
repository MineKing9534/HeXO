@file:Layout(".layout.AppLayout")

package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.session.Session
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.session.hasStarted
import de.mineking.hexo.web.components.Anchor
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.CardHeader
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.ContentCard
import de.mineking.hexo.web.components.EmptyStateCard
import de.mineking.hexo.web.components.LoadingCard
import de.mineking.hexo.web.components.RatedFilter
import de.mineking.hexo.web.components.RatedFilterControl
import de.mineking.hexo.web.components.ScrollableView
import de.mineking.hexo.web.format
import de.mineking.hexo.web.icons.EyeIcon
import de.mineking.hexo.web.icons.RightArrowIcon
import de.mineking.hexo.web.icons.TimeControlIcon
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.pages.games.GameTypeBadge
import de.mineking.hexo.web.rememberHdsRepositories
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@InitRoute
fun initLobbyListPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.SessionList))
}

@Page
@Composable
fun LobbyListPage() {
    val client = rememberHdsRepositories()

    if (client == null) {
        LoadingState()
    } else {
        LobbyList(client.sessionRepository)
    }
}

@Composable
private fun LoadingState() {
    LoadingCard("Connecting to lobby service...")
}

@Composable
private fun LobbyList(sessionRepository: SessionRepository) {
    val sessions by sessionRepository.sessions.collectAsState()
    var filter by remember { mutableStateOf(RatedFilter.All) }
    val sortedLobbies = sessions.values
        .filter { filter.rated == null || it.gameOptions.rated == filter.rated }
        .sortedBy { it.hasStarted() }

    ContentCard({
        classes(
            "flex", "max-h-full", "min-h-0", "flex-col", "gap-4", "overflow-hidden", "p-4", "lg:max-h-[calc(100%-3rem)]",
        )
    }) {
        LobbyListHeader(sortedLobbies.size, filter, onFilterChange = { filter = it })

        if (sortedLobbies.isEmpty()) {
            EmptyLobbyState(filter)
        } else {
            ScrollableView({
                classes("pr-2")
            }) {
                Div({
                    classes(
                        "overflow-hidden", "rounded-xl", "border", "border-slate-800/80", "bg-slate-950/35",
                        "divide-y", "divide-slate-800/80",
                    )
                }) {
                    sortedLobbies.forEach { lobby ->
                        LobbyRow(lobby)
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyListHeader(sessionCount: Int, filter: RatedFilter, onFilterChange: (RatedFilter) -> Unit) {
    Div({ classes("flex", "shrink-0", "flex-wrap", "items-center", "justify-between", "gap-3") }) {
        CardHeader(
            title = "Live sessions",
            supportingText = "Watch public games as they happen",
            iconAttrs = { classes("border-emerald-400/25", "bg-emerald-400/10", "text-emerald-300") },
        ) {
            EyeIcon { classes("size-4") }
        }
        Div({ classes("flex", "w-full", "flex-wrap", "items-center", "justify-between", "gap-2", "sm:w-auto") }) {
            Span({
                classes(
                    "shrink-0", "rounded-full", "border", "border-slate-700/70", "bg-slate-950/50",
                    "px-3", "py-1.5", "text-xs", "text-slate-500",
                )
            }) {
                Span({ classes("mr-1", "font-bold", "text-slate-300") }) {
                    Text("$sessionCount")
                }
                Span {
                    Text(if (sessionCount == 1) "session" else "sessions")
                }
            }
            RatedFilterControl(filter, onFilterChange)
        }
    }
}

@Composable
private fun EmptyLobbyState(filter: RatedFilter) {
    val title = if (filter == RatedFilter.All) "No open lobbies" else "No ${filter.label.lowercase()} sessions"
    val description = if (filter == RatedFilter.All) {
        "There are no public lobbies right now. The sandbox is still available for position analysis."
    } else {
        "There are no open ${filter.label.lowercase()} sessions right now. Try another filter."
    }
    EmptyStateCard(title, description)
}

@Composable
private fun LobbyRow(lobby: Session) {
    Div({
        classes(
            "grid", "gap-4", "p-4", "transition-colors", "duration-200", "hover:bg-slate-800/20",
            "md:grid-cols-[minmax(0,1fr)_auto]", "md:items-center",
        )
    }) {
        Div({ classes("flex", "min-w-0", "flex-col", "gap-2.5") }) {
            Div({ classes("min-w-0") }) {
                H2({ classes("truncate", "text-base", "font-semibold", "text-slate-100") }) {
                    Text(lobby.players.formatPlayers())
                }
                Div({ classes("mt-0.5", "flex", "min-w-0", "items-center", "gap-1.5", "text-xs", "text-slate-600") }) {
                    Span { Text("Session") }
                    Span { Text("·") }
                    Span({ classes("truncate", "font-mono") }) { Text(lobby.id.value) }
                }
            }

            Div({ classes("flex", "flex-wrap", "items-center", "gap-2") }) {
                StatusBadge(lobby)
                GameTypeBadge(lobby.gameOptions, lobby.tournament)
                TimeControlBadge(lobby.gameOptions.timeControl)
            }
        }

        Anchor(AppRoute.Session(lobby.id), {
            classes(
                "group", "inline-flex", "w-full", "shrink-0", "items-center", "justify-center", "gap-2",
                "rounded-lg", "border", "border-slate-700", "bg-slate-800/70", "px-4", "py-2", "text-sm",
                "font-semibold", "text-slate-300", "transition", "hover:border-slate-600", "hover:bg-slate-700/70",
                "hover:text-slate-100", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
                "md:w-auto",
            )
        }) {
            Text("Watch session")
            RightArrowIcon {
                classes("size-4", "shrink-0", "transition-transform", "group-hover:translate-x-0.5")
            }
        }
    }
}

@Composable
private fun StatusBadge(lobby: Session) {
    val isLive = lobby.hasStarted()
    val status = if (isLive) "Live" else "Open"

    Badge(if (isLive) Color.Sky else Color.Emerald, {
        classes("font-semibold")
    }) {
        Span({
            classes("size-1.5", "rounded-full", "bg-current")
            if (isLive) classes("animate-pulse")
            attr("aria-hidden", "true")
        })
        Text(status)
    }
}

@Composable
private fun TimeControlBadge(timeControl: TimeControl) {
    Badge {
        TimeControlIcon {
            classes("h-4", "w-4", "fill-none", "stroke-current")
        }
        Text(timeControl.format())
    }
}

private fun List<Player>.formatPlayers() = when (size) {
    1 -> first().displayName
    else -> joinToString(" vs ") { it.displayName }
}
