package de.mineking.hexo.web.pages.discord

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.web.components.LoadingCard
import de.mineking.hexo.web.layout.PageData
import kotlinx.browser.window

@InitRoute
fun initLinkedRolesPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(null))
}

@Page("/linked-roles")
@Composable
fun LinkedRolesPage() {
    val client = rememberDiscordOAuth2ApiClient()

    LaunchedEffect(Unit) {
        if (AppGlobals.isExporting) return@LaunchedEffect
        window.location.assign(client.createAuthorization(OAuth2Flow.LinkedRoles))
    }

    LoadingCard("Redirecting to Discord...")
}
