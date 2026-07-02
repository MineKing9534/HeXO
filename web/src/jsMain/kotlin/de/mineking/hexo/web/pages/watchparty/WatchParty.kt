package de.mineking.hexo.web.pages.watchparty

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.navigation.Anchor
import com.varabyte.kobweb.navigation.BasePath
import de.mineking.hexo.hds.utils.EntityState
import de.mineking.hexo.sync.common.WatchPartyId
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.components.StatusCard
import de.mineking.hexo.web.components.TextInput
import de.mineking.hexo.web.icons.CheckIcon
import de.mineking.hexo.web.icons.CopyIcon
import de.mineking.hexo.web.icons.EyeIcon
import de.mineking.hexo.web.icons.EyeOffIcon
import de.mineking.hexo.web.icons.PlusIcon
import de.mineking.hexo.web.icons.RightArrowIcon
import de.mineking.hexo.web.layout.AppLayout
import de.mineking.hexo.web.layout.AppPage
import de.mineking.hexo.web.pages.sessions.Session
import de.mineking.hexo.web.rememberWatchPartyController
import de.mineking.hexo.web.session.WatchPartyBoardViewManager
import de.mineking.hexo.web.watchparty.rememberWatchParty
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Page("{id?}")
@Composable
fun WatchPartyPage(ctx: PageContext) {
    val watchPartyController = rememberWatchPartyController()
    val id = WatchPartyId(ctx.route.params["id"].orEmpty())

    AppLayout(activePage = AppPage.WatchParty) {
        if (id.value.isNotEmpty()) {
            WatchParty(id)
        } else if (watchPartyController.hostWatchParty == null) {
            WatchPartyCard()
        } else {
            StatusCard {
                SessionHostOptions()
            }
        }
    }
}

@Composable
fun SessionHostOptions() {
    val watchPartyController = rememberWatchPartyController()
    val hostSession = watchPartyController.hostWatchParty ?: return
    var visible by remember { mutableStateOf(false) }
    val link = "${window.location.origin}${BasePath.prependTo("/watchparty/${hostSession.data.value.id.value}")}"

    H1({ classes("text-2xl", "mb-4", "font-extrabold", "uppercase", "text-slate-200") }) {
        Text("Host Options")
    }

    Div({ classes("flex", "w-full", "flex-col", "gap-2") }) {
        Div({ classes("text-xs", "font-semibold", "uppercase", "text-slate-500") }) {
            Text("Watch Party Link")
        }
        Div({ classes("relative", "w-full") }) {
            TextInput(
                value = link,
                type = InputType.Url,
                readOnly = true,
                monospace = true,
                attrs = {
                    classes("pr-20", "text-ellipsis")
                    if (!visible) style { property("-webkit-text-security", "disc") }
                },
            )
            CopyButton(link)
            VisibleButton(visible = visible, onVisibleChange = { visible = it })
        }
    }

    Div({ classes("mt-5", "flex", "w-full", "flex-col", "gap-2", "border-t", "border-slate-800", "pt-5") }) {
        Button({
            classes(
                "inline-flex", "items-center", "justify-center", "gap-2", "rounded-lg", "border",
                "border-rose-400/40", "bg-rose-500/15", "px-4", "py-3", "font-semibold", "text-rose-300",
                "transition", "cursor-pointer", "hover:bg-rose-500/25", "hover:text-rose-100",
                "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-rose-400/60",
            )
            onClick { watchPartyController.closeHost() }
        }) {
            Text("Stop hosting")
        }
        P({ classes("text-sm", "leading-relaxed", "text-slate-500") }) {
            Text("This only stops hosting from this browser. Active subscribers will not be kicked from the watch party.")
        }
    }
}

@Composable
private fun CopyButton(value: String) {
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied, value) {
        if (!copied) return@LaunchedEffect
        delay(1.5.seconds)
        copied = false
    }

    Button({
        attr("aria-label", if (copied) "Watch party link copied" else "Copy watch party link")
        attr("title", if (copied) "Copied" else "Copy watch party link")
        classes(
            "absolute", "right-11", "top-1/2", "-translate-y-1/2", "grid", "size-8", "place-items-center", "rounded-md",
            "transition", "cursor-pointer", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
        )
        if (copied) {
            classes("text-emerald-300")
        } else {
            classes("text-slate-400", "hover:bg-slate-800", "hover:text-slate-100")
        }
        onClick {
            window.navigator.clipboard.writeText(value)
            copied = true
        }
    }) {
        if (copied) {
            CheckIcon { classes("size-4") }
        } else {
            CopyIcon { classes("size-4") }
        }
    }
}

@Composable
private fun VisibleButton(visible: Boolean, onVisibleChange: (Boolean) -> Unit) {
    Button({
        attr("aria-label", if (visible) "Hide watch party link" else "Show watch party link")
        classes(
            "absolute", "right-2", "top-1/2", "-translate-y-1/2", "grid", "size-8", "place-items-center",
            "rounded-md", "border", "border-transparent", "text-slate-400", "transition", "cursor-pointer",
            "hover:bg-slate-800", "hover:text-slate-100",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
        )
        onClick { onVisibleChange(!visible) }
    }) {
        if (visible) {
            EyeOffIcon { classes("size-4") }
        } else {
            EyeIcon { classes("size-4") }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun WatchPartyCard() {
    val watchPartyController = rememberWatchPartyController()
    var loading by remember { mutableStateOf(false) }

    StatusCard {
        H1({ classes("text-2xl", "font-extrabold", "uppercase", "text-slate-200") }) {
            Text("Join Watch Party")
        }
        JoinSessionInput()

        H1({ classes("mt-4", "text-lg", "font-extrabold", "uppercase", "text-slate-300") }) {
            Text("Or")
        }
        Button({
            classes(
                "group", "inline-flex", "items-center", "justify-center", "gap-2", "rounded-lg", "border",
                "px-4", "py-3", "font-semibold", "text-nowrap", "shadow-sm", "transition", "focus:outline-none",
                "focus-visible:ring-2", "focus-visible:ring-emerald-400/60", "sm:min-w-44",
            )

            if (loading) {
                disabled()
                classes("cursor-wait", "border-slate-700", "bg-slate-800", "text-slate-300", "shadow-none")
            } else {
                classes(
                    "cursor-pointer",
                    "border-emerald-500/40", "bg-emerald-500/15", "text-emerald-100", "shadow-emerald-950/20",
                    "hover:border-emerald-400/60", "hover:bg-emerald-500/25", "hover:text-white",
                )
            }

            onClick {
                if (loading) return@onClick
                loading = true
                GlobalScope.launch {
                    try {
                        watchPartyController.startHost()
                    } finally {
                        loading = false
                    }
                }
            }
        }) {
            if (loading) {
                LoadingIndicator { classes("size-5", "border-4!") }
                Text("Creating...")
            } else {
                PlusIcon {
                    classes("size-4", "shrink-0")
                }
                Text("Host a Watch Party")
            }
        }
    }
}

@Composable
private fun JoinSessionInput() {
    var id by remember { mutableStateOf("") }
    val valid = remember(id) { Uuid.parseOrNull(id) != null }
    var visible by remember { mutableStateOf(false) }

    Div({ classes("flex", "w-full", "flex-col", "gap-2", "sm:flex-row") }) {
        Div({ classes("relative", "w-full") }) {
            TextInput(
                value = id,
                onValueChange = { id = it },
                valid = valid,
                placeholder = "Watch Party Id",
                attrs = {
                    classes("mr-10", "flex-1")
                    if (!visible) style { property("-webkit-text-security", "disc") }
                },
            )
            VisibleButton(visible = visible, onVisibleChange = { visible = it })
        }
        Anchor(BasePath.prependTo("/watchparty/$id"), {
            classes(
                "group", "inline-flex", "items-center", "justify-center", "gap-2", "rounded-lg", "border",
                "px-4", "py-3", "font-semibold", "text-nowrap", "shadow-sm", "transition", "focus:outline-none",
                "focus-visible:ring-2", "focus-visible:ring-emerald-400/60", "sm:min-w-24",
            )

            if (valid) {
                classes(
                    "border-emerald-500/40", "bg-emerald-500/15", "text-emerald-100", "shadow-emerald-950/20",
                    "hover:border-emerald-400/60", "hover:bg-emerald-500/25", "hover:text-white",
                )
            } else {
                attr("aria-disabled", "true")
                classes("pointer-events-none", "border-slate-700", "bg-slate-800", "text-slate-500", "shadow-none")
            }
        }) {
            Span({ classes("whitespace-nowrap", "font-semibold") }) {
                Text("Join")
            }
            RightArrowIcon {
                classes("size-4", "shrink-0", "transition-transform")
                if (valid) classes("group-hover:translate-x-0.5")
            }
        }
    }
}

@Composable
private fun WatchParty(id: WatchPartyId) {
    val state = rememberWatchParty(id)
    when (val state = state) {
        is EntityState.Loading -> LoadingState()
        is EntityState.NotFound -> NotFoundState()
        is EntityState.Data -> {
            val data by state.value.data.collectAsState()
            val sessionId = data.sessionId

            val highlightManager = remember(state.value) { WatchPartyBoardViewManager(state.value) }

            if (sessionId != null) {
                Session(sessionId, highlightManager)
            } else {
                NoSessionState()
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
