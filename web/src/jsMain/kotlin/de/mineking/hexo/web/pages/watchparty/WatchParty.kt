package de.mineking.hexo.web.pages.watchparty

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.RouteInfo
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.watchparty.client.WatchParty
import de.mineking.hexo.watchparty.common.WatchPartyId
import de.mineking.hexo.watchparty.common.WatchPartyTarget
import de.mineking.hexo.web.board.rememberSubscriberBoardViewManager
import de.mineking.hexo.web.components.BackLink
import de.mineking.hexo.web.components.CardHeader
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.LoadingIndicatorSize
import de.mineking.hexo.web.components.NotFoundCard
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.components.SubCardVariant
import de.mineking.hexo.web.icons.BroadcastIcon
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.pages.games.Game
import de.mineking.hexo.web.pages.sandbox.Sandbox
import de.mineking.hexo.web.pages.sessions.Session
import de.mineking.hexo.web.rememberWatchPartyController
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

private val RouteInfo.watchPartyId get() = WatchPartyId(params["id"]!!)

@InitRoute
fun initWatchPartyPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.WatchParty(ctx.route.watchPartyId)))
}

@Page("{id}")
@Composable
fun WatchPartyPage(ctx: PageContext) {
    val watchPartyController = rememberWatchPartyController()
    when (val state = watchPartyController.rememberWatchParty(ctx.route.watchPartyId)) {
        is EntityState.Loading -> LoadingState()
        is EntityState.NotFound -> NotFoundState()
        is EntityState.Data<WatchParty> -> WatchPartyContent(state.value)
    }
}

@Composable
fun WatchPartyContent(watchParty: WatchParty) {
    val data by watchParty.data.collectAsState()
    val boardViewManager = rememberSubscriberBoardViewManager(watchParty)

    when (val target = data.target) {
        is WatchPartyTarget.Session -> Session(target.sessionId, boardViewManager)
        is WatchPartyTarget.Game -> Game(target.gameId, boardViewManager)
        is WatchPartyTarget.Sandbox -> Sandbox(boardViewManager)
        null -> NoSessionState()
    }
}

@Composable
private fun LoadingState() {
    WatchPartyWaitingState(
        title = "Joining watch party",
        supportingText = "Establishing a live connection to the host",
        status = "Connecting to watch party...",
    )
}

@Composable
private fun NotFoundState() {
    NotFoundCard(
        title = "Watch party not found",
        description = "This watch party may have been closed, or the link may be incorrect.",
    ) {
        BackLink(AppRoute.WatchPartyHome, "Back to watch parties")
    }
}

@Composable
private fun NoSessionState() {
    StatusCard({ classes("min-h-0!", "p-5!", "sm:p-8!", "lg:max-w-4xl") }) {
        WatchPartyHero(
            title = "Ready to watch",
            description = "You are connected to the watch party. The shared board will appear here as soon as the host selects one.",
        )
        Div({
            classes(
                "mt-3", "w-full", "rounded-xl", "border", "border-sky-500/30", "bg-sky-500/5",
                "p-4", "text-left", "sm:p-5",
            )
        }) {
            P({ classes("text-xs", "font-bold", "uppercase", "tracking-wider", "text-sky-400") }) {
                Text("Waiting for host")
            }
            H2({ classes("mt-1", "text-xl", "font-bold", "text-slate-100") }) {
                Text("No board attached")
            }
            P({ classes("mt-1", "text-sm", "leading-relaxed", "text-slate-400") }) {
                Text("The host has not shared a game, session, or sandbox position yet.")
            }
            Div({ classes("mt-4", "flex", "items-center", "gap-3", "border-t", "border-sky-500/15", "pt-4") }) {
                LoadingIndicator(LoadingIndicatorSize.Small)
                Span({ classes("text-sm", "font-semibold", "text-slate-300") }) {
                    Text("Listening for board updates...")
                }
            }
        }
    }
}

@Composable
private fun WatchPartyWaitingState(title: String, supportingText: String, status: String) {
    StatusCard(attrs = { classes("min-h-0!", "p-5!", "sm:p-6!") }) {
        Div({ classes("grid", "w-full", "gap-5") }) {
            CardHeader(
                title = title,
                supportingText = supportingText,
                truncateSupportingText = false,
                iconAttrs = { classes("border-sky-400/25", "bg-sky-400/10", "text-sky-300") },
            ) {
                BroadcastIcon { classes("size-4") }
            }
            SubCard({ classes("flex", "items-center", "gap-3", "p-4", "sm:p-5") }, SubCardVariant.Inset) {
                LoadingIndicator(LoadingIndicatorSize.Small)
                Div({ classes("min-w-0") }) {
                    Span({ classes("block", "text-sm", "font-semibold", "text-slate-200") }) {
                        Text(status)
                    }
                    Span({ classes("mt-1", "block", "text-xs", "leading-relaxed", "text-slate-500") }) {
                        Text("Keep this page open; it will update automatically.")
                    }
                }
            }
        }
    }
}
