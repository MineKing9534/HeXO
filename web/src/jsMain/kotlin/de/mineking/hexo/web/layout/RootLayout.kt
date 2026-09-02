package de.mineking.hexo.web.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.data.getValue
import com.varabyte.kobweb.core.layout.Layout
import de.mineking.hexo.watchparty.common.WatchPartyNavigateTarget
import de.mineking.hexo.watchparty.common.WatchPartyTarget
import de.mineking.hexo.web.rememberWatchPartyController
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Layout
@Composable
fun RootLayout(ctx: PageContext, content: @Composable () -> Unit) {
    val data by rememberUpdatedState(ctx.data.getValue<PageData>())

    val watchPartyController = rememberWatchPartyController()
    LaunchedEffect(ctx.route.path) {
        watchPartyController.awaitReady()
        val session = watchPartyController.hostWatchParty ?: return@LaunchedEffect

        session.navigate(when (val route = data.route) {
            is AppRoute.Session -> WatchPartyNavigateTarget.Session(route.id)
            is AppRoute.FinishedGame -> WatchPartyNavigateTarget.Game(route.id)
            is AppRoute.Sandbox -> WatchPartyNavigateTarget.Sandbox
            else -> null
        })
    }

    LaunchedEffect(watchPartyController.hostWatchParty) {
        val watchParty = watchPartyController.hostWatchParty ?: return@LaunchedEffect

        delay(500.milliseconds)
        watchParty.data.collect {
            val route = when (val target = it.target) {
                is WatchPartyTarget.Sandbox -> AppRoute.Sandbox
                is WatchPartyTarget.Game -> AppRoute.FinishedGame(target.gameId)
                is WatchPartyTarget.Session -> AppRoute.Session(target.sessionId)
                null -> AppRoute.SessionList.takeIf {
                    data.route is AppRoute.Sandbox || data.route is AppRoute.Session || data.route is AppRoute.FinishedGame
                }
            } ?: return@collect

            ctx.router.navigateTo(route.href)
        }
    }

    content()
}
