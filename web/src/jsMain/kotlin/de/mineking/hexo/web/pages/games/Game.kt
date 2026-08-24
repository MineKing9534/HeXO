package de.mineking.hexo.web.pages.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.RouteInfo
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.web.board.GameBoardPane
import de.mineking.hexo.web.board.GameBoardViewManager
import de.mineking.hexo.web.board.rememberHostBoardViewManager
import de.mineking.hexo.web.components.BackLink
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.rememberHdsRepositories
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

private val RouteInfo.gameId get() = GameId(params["id"]!!)

@InitRoute
fun initGamePage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.FinishedGame(ctx.route.gameId)))
}

@Page("{id}")
@Composable
fun GamePage(ctx: PageContext) {
    val boardViewManager = rememberHostBoardViewManager<GameBoardViewManager>()
    Game(ctx.route.gameId, boardViewManager)
}

@Composable
fun Game(id: GameId, boardViewManager: GameBoardViewManager) {
    val hdsClient = rememberHdsRepositories()
    if (hdsClient == null) {
        LoadingState()
        return
    }

    var state by remember { mutableStateOf<EntityState<FinishedGameWithPosition>>(EntityState.Loading) }
    LaunchedEffect(id) {
        state = EntityState.Loading

        state = hdsClient.finishedGameRepository.getGame(id)
            ?.let { EntityState.Data(it) }
            ?: EntityState.NotFound
    }

    when (val state = state) {
        is EntityState.Loading -> LoadingState()
        is EntityState.NotFound -> NotFoundState()
        is EntityState.Data -> {
            GameBoardPane(state.value, isLive = false, boardViewManager)
        }
    }
}

@Composable
private fun LoadingState() {
    StatusCard {
        LoadingIndicator { classes("size-9") }
        P({ classes("font-semibold", "text-slate-200") }) {
            Text("Loading game...")
        }
    }
}

@Composable
private fun NotFoundState() {
    StatusCard {
        H1({ classes("text-slate-100", "font-extrabold", "text-3xl", "uppercase") }) {
            Text("Game not found")
        }
        P({ classes("text-slate-400", "text-center") }) {
            Text("The queried game wasn't found.")
        }
        BackLink(AppRoute.FinishedGameList, "Back to games")
    }
}
