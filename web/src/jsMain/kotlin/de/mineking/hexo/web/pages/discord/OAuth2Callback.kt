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
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.navigation.Anchor
import com.varabyte.kobweb.navigation.BasePath
import de.mineking.hexo.discord.oauth2.model.OAuth2CallbackResponse
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.web.components.CardHeader
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.LoadingIndicatorSize
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.components.SubCard
import de.mineking.hexo.web.components.SubCardVariant
import de.mineking.hexo.web.icons.AlertTriangleIcon
import de.mineking.hexo.web.icons.CheckIcon
import de.mineking.hexo.web.layout.PageData
import de.mineking.hexo.web.rememberAuthenticationController
import de.mineking.hexo.web.rememberDiscordOAuth2Client
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.url.URL

@InitRoute
fun initOAuth2CallbackPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(null))
}

@Page("/oauth2/callback")
@Composable
fun OAuth2CallbackPage(ctx: PageContext) {
    var result by remember { mutableStateOf<OAuth2CallbackResponse?>(null) }
    val discordOAuth2ApiClient = rememberDiscordOAuth2Client()

    val code = ctx.route.queryParams["code"].orEmpty()
    val state = ctx.route.queryParams["state"].orEmpty()

    LaunchedEffect(code, state) {
        if (AppGlobals.isExporting) return@LaunchedEffect
        val url = URL(window.location.href)
        url.searchParams.delete("code")
        url.searchParams.delete("state")
        window.history.replaceState(null, "", url.toString())

        result = null
        result = runCatching {
            discordOAuth2ApiClient.completeAuthorization(code, state)
        }.getOrElse {
            OAuth2CallbackResponse(success = false, flow = null)
        }
    }

    when (val currentResult = result) {
        null -> OAuth2LoadingCard()
        else -> OAuth2FlowResult(currentResult)
    }
}

@Composable
private fun OAuth2LoadingCard() {
    StatusCard(attrs = { classes("lg:max-w-3xl") }) {
        Div({ classes("w-full") }) {
            CardHeader(
                title = "Completing Discord authorization",
                supportingText = "HeXO is securely finishing the connection with Discord.",
                truncateSupportingText = false,
                iconAttrs = { classes("border-emerald-400/20", "bg-emerald-400/10", "text-emerald-300") },
            ) {
                LoadingIndicator(LoadingIndicatorSize.Tiny)
            }
            SubCard({ classes("mt-5", "p-4", "sm:p-5") }, SubCardVariant.Inset) {
                P({ classes("text-sm", "leading-relaxed", "text-slate-300") }) {
                    Text("Keep this page open. You will see a confirmation as soon as Discord authorization is complete.")
                }
            }
        }
    }
}

@Composable
private fun OAuth2FlowResult(result: OAuth2CallbackResponse) {
    when (result.flow) {
        OAuth2Flow.LinkedRoles -> LinkedRolesOAuth2Result(result.success)
        OAuth2Flow.WebLogin -> WebLoginOAuth2Result(result.success)
        null -> UnknownOAuth2Result(result.success)
    }
}

@Composable
private fun LinkedRolesOAuth2Result(success: Boolean) {
    OAuth2ResultCard(
        success = success,
        title = if (success) "Discord linked roles authorized" else "Linked-role authorization failed",
        supportingText = if (success) {
            "HeXO can now keep your Discord linked-role data up to date."
        } else {
            "HeXO could not get permission to update your Discord linked-role data."
        },
        retryPath = if (success) null else "/linked-roles",
    ) {
        if (success) {
            Text("Your rating and rank can now be used for Discord linked roles. You can close this tab and return to Discord.")

            P({ classes("mt-3", "text-sm", "leading-relaxed", "text-slate-400") }) {
                Text("Your HeXO profile must also be connected before Discord can assign linked roles. Check both statuses with ")
                Span({ classes("font-mono", "font-semibold", "text-emerald-300", "mx-0.5") }) { Text("/link") }
                Text(" in Discord.")
            }
        } else {
            Text(
                "The authorization request may have expired or been cancelled. Start the linked-role flow again to request a new authorization link.",
            )
        }
    }
}

@Composable
private fun WebLoginOAuth2Result(success: Boolean) {
    val authenticationController = rememberAuthenticationController()
    val context = rememberPageContext()

    if (success) {
        LaunchedEffect(authenticationController) {
            val controller = authenticationController ?: return@LaunchedEffect
            controller.markUpdated()
            context.router.navigateTo(BasePath.prependTo("/"))
        }
        OAuth2LoadingCard()
        return
    }

    OAuth2ResultCard(
        success = false,
        title = "Sign-in failed",
        supportingText = "HeXO could not sign you in with Discord.",
    ) {
        Text("The login request may have expired or been cancelled. Return to HeXO and start the login again.")
    }
}

@Composable
private fun UnknownOAuth2Result(success: Boolean) {
    OAuth2ResultCard(
        success = success,
        title = if (success) "Discord authorization complete" else "Discord authorization failed",
        supportingText = if (success) {
            "HeXO successfully connected to Discord."
        } else {
            "HeXO could not finish the connection with Discord."
        },
    ) {
        if (success) {
            Text("You can close this tab and return to Discord.")
        } else {
            Text("The authorization request may have expired or been cancelled. Start the flow again to request a new authorization link.")
        }
    }
}

@Composable
private fun OAuth2ResultCard(
    success: Boolean,
    title: String,
    supportingText: String,
    retryPath: String? = null,
    explanation: @Composable () -> Unit,
) {
    StatusCard(attrs = { classes("lg:max-w-3xl") }) {
        Div({ classes("w-full") }) {
            CardHeader(
                title = title,
                supportingText = supportingText,
                truncateSupportingText = false,
                iconAttrs = {
                    if (success) {
                        classes("border-emerald-400/30", "bg-emerald-400/10", "text-emerald-300")
                    } else {
                        classes("border-rose-400/30", "bg-rose-400/10", "text-rose-300")
                    }
                },
            ) {
                if (success) CheckIcon { classes("size-5") } else AlertTriangleIcon { classes("size-5") }
            }

            SubCard({ classes("mt-5", "p-4", "sm:p-5") }, SubCardVariant.Inset) {
                Div({ classes("text-sm", "leading-relaxed", "text-slate-300") }) {
                    explanation()
                }
            }

            if (retryPath != null) {
                Div({ classes("mt-5", "flex", "justify-end") }) {
                    Anchor(BasePath.prependTo(retryPath), {
                        classes(
                            "inline-flex", "items-center", "justify-center", "rounded-lg", "border", "px-4", "py-2",
                            "text-sm", "font-semibold", "transition", "border-emerald-400/35", "bg-emerald-500/15",
                            "text-emerald-200", "hover:bg-emerald-500/25", "hover:text-emerald-100",
                            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
                        )
                    }) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}
