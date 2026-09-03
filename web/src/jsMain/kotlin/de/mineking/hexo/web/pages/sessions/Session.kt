package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.RouteInfo
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import de.mineking.hexo.game.model.EntityState
import de.mineking.hexo.game.model.session.LiveSession
import de.mineking.hexo.game.model.session.LobbySession
import de.mineking.hexo.game.model.session.Session
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.game.model.session.SessionState
import de.mineking.hexo.web.audio.SoundEffect
import de.mineking.hexo.web.board.GameBoardPane
import de.mineking.hexo.web.board.GameBoardViewManager
import de.mineking.hexo.web.board.rememberHostBoardViewManager
import de.mineking.hexo.web.components.BackLink
import de.mineking.hexo.web.components.LoadingCard
import de.mineking.hexo.web.components.NotFoundCard
import de.mineking.hexo.web.layout.AppRoute
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.rememberHdsRepositories
import de.mineking.hexo.web.rememberPrevious
import de.mineking.hexo.web.rememberSoundPlayer

private val RouteInfo.sessionId get() = SessionId(params["id"]!!)

@InitRoute
fun initSessionPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(AppRoute.Session(ctx.route.sessionId)))
}

@Page("{id}")
@Composable
fun SessionPage(ctx: PageContext) {
    val boardViewManager = rememberHostBoardViewManager<GameBoardViewManager>()
    Session(ctx.route.sessionId, boardViewManager)
}

@Composable
fun Session(id: SessionId, boardViewManager: GameBoardViewManager) {
    val hdsClient = rememberHdsRepositories()
    if (hdsClient == null) {
        LoadingState()
        return
    }

    val state by remember(id) { hdsClient.sessionRepository.observeSession(id) }.collectAsState()
    when (val state = state) {
        is EntityState.Loading -> LoadingState()
        is EntityState.NotFound -> NotFoundState()
        is EntityState.Data -> {
            SessionSounds(state.value)

            when (val session = state.value) {
                is LiveSession -> {
                    val state = session.state

                    GameBoardPane(
                        game = session.game,
                        isLive = state is SessionState.InGame,
                        boardViewManager = boardViewManager,
                    )
                    if (state is SessionState.Detailed.Finished) SessionFinishedOverlay(session, state)
                }
                is LobbySession -> LobbyOverlay(session)
            }
        }
    }
}

@Composable
private fun SessionSounds(session: Session) {
    val soundPlayer = rememberSoundPlayer()
    val previousState = rememberPrevious(session.id, session.state)

    if (session is LiveSession) {
        val previousMoveCount = rememberPrevious(session.id, session.game.moveCount)

        LaunchedEffect(session.game.moveCount) {
            if (previousMoveCount != null && session.game.moveCount > previousMoveCount) {
                soundPlayer.play(SoundEffect.TilePlaced)
            }
        }
    }

    LaunchedEffect(session.state) {
        if (session.state is SessionState.InGame && previousState !is SessionState.InGame) {
            soundPlayer.play(SoundEffect.GameStart)
        }
        if (session.state is SessionState.Finished && previousState is SessionState.InGame) {
            soundPlayer.play(SoundEffect.GameWin)
        }
    }
}

@Composable
private fun LoadingState() {
    LoadingCard("Connecting to session...")
}

@Composable
private fun NotFoundState() {
    NotFoundCard(
        title = "Session not found",
        description = "This session may have finished, been closed, or the link may be incorrect.",
    ) {
        BackLink(AppRoute.SessionList, "Back to lobbies")
    }
}
