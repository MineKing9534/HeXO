@file:Layout(".layout.AppLayout")

package de.mineking.hexo.web.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import de.mineking.hexo.web.pages.sessions.LobbyListPage
import de.mineking.hexo.web.pages.sessions.initLobbyListPage

@InitRoute
fun initIndexPage(ctx: InitRouteContext) {
    initLobbyListPage(ctx)
}

@Page
@Composable
fun IndexPage() {
    LobbyListPage()
}
