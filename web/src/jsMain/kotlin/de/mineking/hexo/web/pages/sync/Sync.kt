package de.mineking.hexo.web.pages.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import de.mineking.hexo.hds.utils.EntityState
import de.mineking.hexo.sync.common.SessionSyncId
import de.mineking.hexo.web.components.AppLayout
import de.mineking.hexo.web.components.AppPage
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.pages.sessions.Session
import de.mineking.hexo.web.session.SyncHighlightManager
import de.mineking.hexo.web.sync.rememberSyncSession
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Page("{id}")
@Composable
fun SyncPage(ctx: PageContext) {
    val id = remember { SessionSyncId(ctx.route.params["id"]!!) }

    val state = rememberSyncSession(id)
    AppLayout(activePage = AppPage.Sync) {
        when (val state = state) {
            is EntityState.Loading -> LoadingState()
            is EntityState.NotFound -> NotFoundState()
            is EntityState.Data -> {
                val data by state.value.data.collectAsState()
                val sessionId = data.sessionId

                val highlightManager = remember(state.value) { SyncHighlightManager(state.value) }

                if (sessionId != null) {
                    Session(sessionId, highlightManager)
                } else {
                    NoSessionState()
                }
            }
        }
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
            Text("No live session attached")
        }

        LoadingIndicator { classes("size-9") }
        P({ classes("font-semibold", "text-slate-200") }) {
            Text("Waiting for host to attach a session...")
        }
    }
}
