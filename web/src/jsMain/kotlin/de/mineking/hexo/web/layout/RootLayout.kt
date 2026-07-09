package de.mineking.hexo.web.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.data.getValue
import com.varabyte.kobweb.core.layout.Layout
import de.mineking.hexo.sync.common.WatchPartyNavigateTarget
import de.mineking.hexo.web.rememberWatchPartyController

@Layout
@Composable
fun RootLayout(ctx: PageContext, content: @Composable () -> Unit) {
    val data = ctx.data.getValue<PageData>()

    val watchPartyController = rememberWatchPartyController()
    LaunchedEffect(ctx.route.path) {
        val session = watchPartyController.hostWatchParty ?: return@LaunchedEffect

        session.navigate(when (data.route) {
            is AppRoute.Session -> WatchPartyNavigateTarget.Session(data.route.id)
            is AppRoute.Sandbox -> WatchPartyNavigateTarget.Sandbox
            else -> null
        })
    }

    key(ctx.route.path) {
        content()
    }
}
