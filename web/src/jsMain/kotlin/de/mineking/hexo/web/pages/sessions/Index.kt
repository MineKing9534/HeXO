@file:Layout(".layout.AppLayout")

package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.Player
import de.mineking.hexo.game.model.session.LobbySession
import de.mineking.hexo.game.model.session.SessionRepository
import de.mineking.hexo.game.model.session.hasStarted
import de.mineking.hexo.web.components.Anchor
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.ContentCard
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.ScrollableView
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.format
import de.mineking.hexo.web.icons.CasualGameIcon
import de.mineking.hexo.web.icons.StarIcon
import de.mineking.hexo.web.icons.TimeControlIcon
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.rememberHdsRepositories
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@InitRoute
fun initLobbyListPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.LobbyList))
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
    StatusCard {
        LoadingIndicator { classes("size-9") }
        P({ classes("font-semibold", "text-slate-200") }) {
            Text("Connecting to lobby service...")
        }
    }
}

@Composable
private fun LobbyList(sessionRepository: SessionRepository) {
    val lobbies by sessionRepository.lobbies.collectAsState()
    val sortedLobbies = lobbies.values.sortedWith(
        compareByDescending<LobbySession> { it.hasStarted() }
            .thenByDescending { it.createdAt },
    )

    ContentCard({
        classes(
            "flex", "max-h-full", "min-h-0", "flex-col", "gap-4", "overflow-hidden", "p-4", "lg:max-h-[calc(100%-3rem)]",
        )
    }) {
        Div({ classes("flex", "shrink-0", "items-center", "justify-between", "gap-3") }) {
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
            ScrollableView({
                classes("pr-2")
            }) {
                Div({ classes("grid", "gap-4") }) {
                    sortedLobbies.forEach { lobby ->
                        LobbyCard(lobby)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLobbyState() {
    SubCard({
        classes("grid", "min-h-64", "place-items-center", "border-dashed", "bg-slate-950/40", "p-6", "text-center")
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
    Anchor(AppRoute.Session(lobby.id), {
        classes("block")
    }) {
        SubCard({
            classes(
                "group", "grid", "gap-4", "p-3", "transition", "hover:border-slate-600/80",
                "hover:bg-slate-700/80", "md:grid-cols-[1fr_auto]", "md:items-center",
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
}

@Composable
private fun StatusBadge(lobby: LobbySession) {
    val status = if (lobby.hasStarted()) "In game" else "Waiting"

    Badge(if (lobby.hasStarted()) Color.Sky else Color.Emerald, {
        classes("font-semibold")
    }) {
        Text(status)
    }
}

@Composable
private fun GameTypeBadge(rated: Boolean) {
    Badge(if (rated) Color.Yellow else Color.Neutral) {
        GameTypeIcon(rated)
        Text(if (rated) "Rated" else "Casual")
    }
}

@Composable
private fun GameTypeIcon(rated: Boolean) {
    if (rated) {
        StarIcon {
            classes("h-3.5", "w-3.5", "fill-current")
        }
    } else {
        CasualGameIcon {
            classes("h-3.5", "w-3.5", "fill-none", "stroke-current")
        }
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
