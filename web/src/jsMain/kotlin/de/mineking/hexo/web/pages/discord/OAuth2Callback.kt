package de.mineking.hexo.web.pages.discord

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.web.components.LoadingCard
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.layout.PageData
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@InitRoute
fun initOAuth2CallbackPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(null))
}

@Page("/oauth2/callback")
@Composable
fun OAuth2CallbackPage(ctx: PageContext) {
    var result by remember { mutableStateOf<Boolean?>(null) }
    val client = rememberDiscordOAuth2ApiClient()

    LaunchedEffect(Unit) {
        if (AppGlobals.isExporting) return@LaunchedEffect
        result = client.completeAuthorization(ctx.route.queryParams["code"] ?: "", ctx.route.queryParams["state"] ?: "")
    }

    when (result) {
        null -> LoadingCard("Connecting Discord account...")
        true -> OAuth2ResultCard(success = true)
        false -> OAuth2ResultCard(success = false)
    }
}

@Composable
private fun OAuth2ResultCard(success: Boolean) {
    StatusCard {
        H1({ classes("text-center", "text-xl", "font-bold", if (success) "text-emerald-300" else "text-red-300") }) {
            Text(if (success) "Account Linked Successfully" else "Discord Authorization Failed")
        }
        P({ classes("max-w-xl", "text-center", "text-sm", "leading-relaxed", "text-slate-300") }) {
            Text(
                if (success) {
                    "Your Discord account is now connected. You can close this tab and return to Discord."
                } else {
                    "We could not complete your Discord authorization. Start the linking flow from Discord again and finish it before the link expires."
                },
            )
        }
        if (success) {
            P({
                classes(
                    "max-w-xl", "rounded-lg", "border", "border-amber-300/25", "bg-amber-500/10", "px-4", "py-3",
                    "text-sm", "text-amber-100",
                )
            }) {
                Span({ classes("mr-1", "font-semibold", "uppercase", "tracking-wide", "text-amber-200") }) { Text("Note:") }
                Text("Make sure your HeXO profile is linked to this Discord account before claiming roles. Use the ")
                Span({ classes("font-mono", "font-semibold", "text-amber-300") }) { Text("/link") }
                Text(" command in Discord.")
            }
        }
    }
}
