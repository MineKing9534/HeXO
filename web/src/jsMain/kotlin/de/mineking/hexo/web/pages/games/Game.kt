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
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.utils.types.map
import de.mineking.hexo.utils.types.orElse
import de.mineking.hexo.web.board.GameBoardPane
import de.mineking.hexo.web.board.GameBoardViewManager
import de.mineking.hexo.web.board.rememberHostBoardViewManager
import de.mineking.hexo.web.components.BackLink
import de.mineking.hexo.web.components.LoadingCard
import de.mineking.hexo.web.components.NotFoundCard
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.pages.sessions.FinishedGameOverlay
import de.mineking.hexo.web.rememberHdsRepositories

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
            .map { EntityState.Data(it) }
            .orElse { EntityState.NotFound }
    }

    when (val state = state) {
        is EntityState.Loading -> LoadingState()
        is EntityState.NotFound -> NotFoundState()
        is EntityState.Data -> {
            GameBoardPane(state.value, isLive = false, boardViewManager = boardViewManager)
            FinishedGameOverlay(state.value)
        }
    }
}

@Composable
private fun LoadingState() {
    LoadingCard("Loading game...")
}

@Composable
private fun NotFoundState() {
    NotFoundCard(
        title = "Game not found",
        description = "This game does not exist, may have been removed, or the link may be incorrect.",
    ) {
        BackLink(AppRoute.FinishedGameList, "Back to games")
    }
}
