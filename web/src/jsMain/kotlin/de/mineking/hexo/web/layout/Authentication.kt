package de.mineking.hexo.web.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.utils.types.EntityState
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.LoadingIndicatorSize
import de.mineking.hexo.web.icons.ChevronDownIcon
import de.mineking.hexo.web.icons.DisconnectIcon
import de.mineking.hexo.web.icons.DiscordIcon
import de.mineking.hexo.web.rememberAuthenticationController
import de.mineking.hexo.web.rememberDiscordOAuth2Client
import de.mineking.hexo.web.rememberHmdRepositories
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.Node
import org.w3c.dom.events.EventListener

@Composable
fun AuthenticationControl() {
    val profileRepository = rememberHmdRepositories()?.profileRepository
    val coroutineScope = rememberCoroutineScope()

    val state = profileRepository?.currentProfile?.collectAsState()

    when (val state = state?.value ?: EntityState.Loading) {
        is EntityState.Loading -> LoadingState()
        is EntityState.NotFound -> LogInButton()
        is EntityState.Data -> LoggedInState(state.value, coroutineScope)
    }
}

@Composable
private fun LoggedInState(profile: Profile, coroutineScope: CoroutineScope) {
    var accountMenuOpen by remember { mutableStateOf(false) }

    Div({
        classes("relative", "min-w-0")
        ref { element ->
            val pointerDown = EventListener { event ->
                val target = event.target
                if (target !is Node || !element.contains(target)) accountMenuOpen = false
            }

            document.addEventListener("pointerdown", pointerDown)
            onDispose { document.removeEventListener("pointerdown", pointerDown) }
        }
    }) {
        AccountMenuButton(profile, accountMenuOpen) { accountMenuOpen = !accountMenuOpen }
        if (accountMenuOpen) AccountMenu(profile, coroutineScope) { accountMenuOpen = false }
    }
}

@Composable
private fun AccountMenuButton(profile: Profile, menuOpen: Boolean, onClick: () -> Unit) {
    Button({
        classes(
            "group", "flex", "min-w-0", "shrink-0", "cursor-pointer", "items-center", "gap-2.5", "rounded-lg", "border",
            "border-slate-700/60", "bg-slate-900/70", "p-1.5", "text-left", "shadow-sm", "shadow-black/20",
            "transition", "hover:border-slate-600", "hover:bg-slate-800/80", "focus:outline-none",
            "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
        )
        if (menuOpen) classes("border-slate-600", "bg-slate-800/80")
        attr("aria-label", "Open account menu for ${profile.displayName}")
        attr("aria-expanded", menuOpen.toString())
        attr("aria-haspopup", "menu")
        onClick { onClick() }
    }) {
        ProfileAvatar(profile, compact = true)
        Span({ classes("min-w-0", "max-w-28", "flex-1", "leading-tight", "sm:max-w-36") }) {
            Span({ classes("block", "text-[10px]", "font-medium", "text-slate-500") }) { Text("Signed in as") }
            Span({ classes("mt-0.5", "block", "truncate", "text-xs", "font-bold", "text-slate-100") }) {
                Text(profile.displayName)
            }
        }
        ChevronDownIcon {
            classes("size-3.5", "shrink-0", "text-slate-500", "transition-transform")
            if (menuOpen) classes("rotate-180", "text-slate-300")
        }
    }
}

@Composable
private fun AccountMenu(profile: Profile, coroutineScope: CoroutineScope, closeMenu: () -> Unit) {
    Div({
        classes(
            "absolute", "right-0", "top-[calc(100%+0.5rem)]", "z-40", "w-64", "overflow-hidden",
            "rounded-xl", "border", "border-slate-700", "bg-slate-950/98", "shadow-2xl",
            "shadow-black/50", "backdrop-blur-md",
        )
        attr("role", "menu")
    }) {
        AccountMenuHeader(profile)
        LogOutButton(coroutineScope, closeMenu)
    }
}

@Composable
private fun AccountMenuHeader(profile: Profile) {
    Div({ classes("flex", "min-w-0", "items-center", "gap-3", "border-b", "border-slate-800", "p-3") }) {
        ProfileAvatar(profile, compact = false)
        Span({ classes("min-w-0") }) {
            Span({ classes("block", "truncate", "text-sm", "font-bold", "text-slate-100") }) {
                Text(profile.displayName)
            }
            Span({ classes("mt-1", "flex", "items-center", "gap-1.5", "text-xs", "text-slate-500") }) {
                DiscordIcon { classes("size-3.5", "shrink-0") }
                Text("Discord account")
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: Profile, compact: Boolean) {
    Span({
        classes(
            "grid", if (compact) "size-8" else "size-10", "shrink-0", "place-items-center", "overflow-hidden",
            "rounded-full", "bg-slate-800", if (compact) "text-xs" else "text-sm", "font-bold", "text-slate-200",
            "ring-1", if (compact) "ring-slate-600/70" else "ring-slate-600",
        )
    }) {
        val image = profile.image
        if (image != null) {
            Img(image, if (compact) profile.displayName else "") { classes("size-full", "object-cover") }
        } else {
            Text(profile.displayName.firstOrNull()?.uppercase() ?: "?")
        }
    }
}

@Composable
private fun LogOutButton(coroutineScope: CoroutineScope, closeMenu: () -> Unit) {
    val authenticationController = rememberAuthenticationController()
    var loading by remember { mutableStateOf(false) }

    Div({ classes("p-1.5") }) {
        Button({
            classes(
                "flex", "w-full", "cursor-pointer", "items-center", "gap-2", "rounded-lg", "px-2.5", "py-2",
                "text-left", "text-sm", "font-semibold", "text-rose-300", "transition", "hover:bg-rose-500/10",
                "hover:text-rose-200", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-rose-400/50",
                "disabled:cursor-wait", "disabled:opacity-60",
            )
            attr("type", "button")
            attr("role", "menuitem")
            if (loading) disabled()
            onClick {
                if (loading) return@onClick
                loading = true
                closeMenu()
                coroutineScope.launch {
                    try {
                        val _ = authenticationController?.logout()
                    } finally {
                        loading = false
                    }
                }
            }
        }) {
            DisconnectIcon { classes("size-4", "shrink-0") }
            Text(if (loading) "Logging out…" else "Log out")
        }
    }
}

@Composable
private fun LoadingState() {
    Div({
        classes(
            "inline-flex", "items-center", "justify-center", "gap-2", "rounded-lg", "border", "border-slate-700/50",
            "bg-slate-900/50", "px-3", "py-2", "text-xs", "font-semibold", "text-slate-400", "shadow-sm",
            "shadow-black/20",
        )
        attr("role", "status")
        attr("aria-live", "polite")
    }) {
        LoadingIndicator(LoadingIndicatorSize.Tiny) { classes("border-t-slate-300!") }
        Text("Checking session…")
    }
}

@Composable
private fun LogInButton() {
    val discordOAuth2ApiClient = rememberDiscordOAuth2Client()
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }

    Button({
        classes(
            "inline-flex", "cursor-pointer", "items-center", "justify-center", "gap-2", "rounded-lg", "border",
            "border-[#5865F2]", "bg-[#5865F2]", "px-3", "py-2", "text-xs", "font-bold", "text-white",
            "shadow-sm", "shadow-indigo-950/30", "transition", "hover:border-[#4752C4]", "hover:bg-[#4752C4]",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-[#5865F2]/70",
            "disabled:cursor-wait", "disabled:opacity-70",
        )
        if (loading) disabled()
        onClick {
            if (loading) return@onClick
            loading = true
            scope.launch {
                try {
                    val url = discordOAuth2ApiClient.createAuthorization(OAuth2Flow.WebLogin)
                    window.location.assign(url)
                } finally {
                    loading = false
                }
            }
        }
    }) {
        DiscordIcon { classes("size-4", "shrink-0") }
        Text(if (loading) "Connecting…" else "Log in")
    }
}
