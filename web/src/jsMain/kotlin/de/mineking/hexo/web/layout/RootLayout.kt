package de.mineking.hexo.web.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.navigation.BasePath
import de.mineking.hexo.hds.session.SessionId
import de.mineking.hexo.web.rememberWatchPartyController

private val sessionRoutePattern = "^${BasePath.value}sessions/(.*)$".toRegex()

@Layout
@Composable
fun RootLayout(ctx: PageContext, content: @Composable () -> Unit) {
    val watchPartyController = rememberWatchPartyController()
    LaunchedEffect(ctx.route.path) {
        val session = watchPartyController.hostWatchParty ?: return@LaunchedEffect

        val match = sessionRoutePattern.find(ctx.route.path)
        if (match == null) {
            session.navigate(null)
            return@LaunchedEffect
        }

        session.navigate(SessionId(match.groupValues[1]))
    }

    key(ctx.route.path) {
        content()
    }
}
