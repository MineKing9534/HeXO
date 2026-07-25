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
import de.mineking.hexo.hds.utils.EntityState
import de.mineking.hexo.sync.client.WatchParty
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.sync.common.WatchPartyTarget
import de.mineking.hexo.web.board.rememberSubscriberBoardViewManager
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.pages.Sandbox
import de.mineking.hexo.web.pages.games.Game
import de.mineking.hexo.web.pages.sessions.Session
import de.mineking.hexo.web.rememberWatchPartyController
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
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
        is EntityState.Data -> WatchPartyContent(state.value)
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
    StatusCard {
        LoadingIndicator { classes("size-9") }
        P({ classes("font-semibold", "text-slate-200") }) {
            Text("Connecting to watch party...")
        }
    }
}

@Composable
private fun NotFoundState() {
    StatusCard {
        H1({ classes("text-slate-100", "font-extrabold", "text-3xl", "uppercase") }) {
            Text("Watch party not found")
        }
        P({ classes("text-slate-400", "text-center") }) {
            Text("This synchronization session doesn't exist. It may have been closed or the link may be correct.")
        }
    }
}

@Composable
private fun NoSessionState() {
    StatusCard {
        H1({ classes("text-slate-100", "font-extrabold", "text-3xl", "uppercase", "mb-8") }) {
            Text("No board attached")
        }

        LoadingIndicator { classes("size-9") }
        P({ classes("font-semibold", "text-slate-200") }) {
            Text("Waiting for host to attach a session...")
        }
    }
}
