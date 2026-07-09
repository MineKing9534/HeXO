package de.mineking.hexo.web.pages.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.navigation.Anchor
import de.mineking.hexo.hds.session.LiveSession
import de.mineking.hexo.hds.session.LobbySession
import de.mineking.hexo.hds.session.Session
import de.mineking.hexo.hds.session.SessionId
import de.mineking.hexo.hds.session.SessionState
import de.mineking.hexo.hds.utils.EntityState
import de.mineking.hexo.web.audio.SoundEffect
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.icons.ChevronLeftIcon
import de.mineking.hexo.web.layout.AppLayout
import de.mineking.hexo.web.layout.AppPage
import de.mineking.hexo.web.rememberHdsApiClient
import de.mineking.hexo.web.rememberPrevious
import de.mineking.hexo.web.rememberSoundPlayer
import de.mineking.hexo.web.rememberWatchPartyController
import de.mineking.hexo.web.session.BoardViewManager
import de.mineking.hexo.web.session.LobbyOverlay
import de.mineking.hexo.web.session.LocalBoardViewManager
import de.mineking.hexo.web.session.SessionBoardPane
import de.mineking.hexo.web.session.SessionFinishedOverlay
import de.mineking.hexo.web.session.WatchPartyBoardViewManager
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Page("{id}")
@Composable
fun SessionPage(ctx: PageContext) {
    val watchPartyController = rememberWatchPartyController()
    val id = SessionId(ctx.route.params["id"]!!)

    AppLayout(activePage = AppPage.Lobbies) {
        val highlightManager = remember(watchPartyController.hostWatchParty) {
            watchPartyController.hostWatchParty
                ?.let { WatchPartyBoardViewManager(it) }
                ?: LocalBoardViewManager()
        }
        Session(id, highlightManager)
    }
}

@Composable
fun Session(id: SessionId, boardViewManager: BoardViewManager) {
    val hdsClient = rememberHdsApiClient()
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

                    SessionBoardPane(session, state as? SessionState.InGame, boardViewManager)
                    if (state is SessionState.Finished) SessionFinishedOverlay(session, state)
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
    StatusCard {
        LoadingIndicator { classes("size-9") }
        P({ classes("font-semibold", "text-slate-200") }) {
            Text("Connecting to session...")
        }
    }
}

@Composable
private fun NotFoundState() {
    StatusCard {
        H1({ classes("text-slate-100", "font-extrabold", "text-3xl", "uppercase") }) {
            Text("Session not found")
        }
        P({ classes("text-slate-400", "text-center") }) {
            Text("This live session does not exist anymore. It may have finished already, been closed, or the link may be incorrect.")
        }
        BackToLobbiesLink()
    }
}

@Composable
fun BackToLobbiesLink() {
    Anchor("/", {
        classes(
            "group", "inline-flex", "items-center", "gap-2", "rounded-lg", "border", "px-4", "py-2", "text-sm", "font-semibold", "shadow-sm",
            "border-emerald-500/40", "bg-emerald-500/10", "text-emerald-100", "shadow-emerald-950/20", "transition",
            "hover:border-emerald-400/60", "hover:bg-emerald-500/20", "hover:text-white",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60", "mt-2",
        )
    }) {
        ChevronLeftIcon {
            classes("size-4", "shrink-0", "transition-transform", "group-hover:-translate-x-0.5")
        }
        Span({ classes("whitespace-nowrap") }) {
            Text("Back to lobbies")
        }
    }
}
