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
import de.mineking.hexo.web.components.CardHeader
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.LoadingIndicatorSize
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.components.SubCardVariant
import de.mineking.hexo.web.icons.DiscordIcon
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.rememberDiscordOAuth2Client
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@InitRoute
fun initLinkedRolesPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(null))
}

@Page("/linked-roles")
@Composable
fun LinkedRolesPage() {
    val client = rememberDiscordOAuth2Client()

    LaunchedEffect(Unit) {
        if (AppGlobals.isExporting) return@LaunchedEffect
        window.location.assign(client.createAuthorization(OAuth2Flow.LinkedRoles))
    }

    StatusCard(attrs = { classes("lg:max-w-3xl") }) {
        Div({ classes("w-full") }) {
            CardHeader(
                title = "Authorize Discord linked roles",
                supportingText = "Connect HeXO to Discord to keep your linked-role data up to date.",
                truncateSupportingText = false,
                iconAttrs = { classes("border-[#5865F2]", "bg-[#5865F2]", "text-white") },
            ) {
                DiscordIcon { classes("size-5") }
            }

            SubCard({ classes("mt-5", "flex", "items-start", "gap-4", "p-4", "sm:p-5") }, SubCardVariant.Inset) {
                Span({ classes("mt-0.5", "grid", "size-8", "shrink-0", "place-items-center", "text-emerald-300") }) {
                    LoadingIndicator(LoadingIndicatorSize.Small)
                }
                Div {
                    P({ classes("font-semibold", "text-slate-200") }) { Text("Opening Discord authorization") }
                    P({ classes("mt-1", "text-sm", "leading-relaxed", "text-slate-400") }) {
                        Text(
                            "Discord will ask you to approve access to your linked-role data. You will return here automatically when authorization is complete.",
                        )
                    }
                }
            }
        }
    }
}
