@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.web.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.data.getValue
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.navigation.BasePath
import de.mineking.hexo.web.components.Anchor
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.components.LoadingIndicator
import de.mineking.hexo.web.icons.BroadcastIcon
import de.mineking.hexo.web.icons.ChevronRightIcon
import de.mineking.hexo.web.icons.GitHubIcon
import de.mineking.hexo.web.icons.SettingsIcon
import de.mineking.hexo.web.pages.watchparty.WatchPartyHostOptions
import de.mineking.hexo.web.rememberWatchPartyController
import de.mineking.hexo.web.settings.SettingsView
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

private const val GITHUB_URL = "https://github.com/MineKing9534/HeXO-Renderer"

enum class PageStyle(val containerClasses: List<String>) {
    Default(listOf("p-3", "md:p-6")),
    Raw(emptyList()),
}

data class PageData(
    val route: AppRoute?,
    val style: PageStyle = PageStyle.Default,
)

class AppLayout(
    fullscreen: MutableState<Boolean>,
    pageStyle: MutableState<PageStyle>,
) {
    var fullscreen by fullscreen
    var pageStyle by pageStyle
}

private val LocalAppLayout = staticCompositionLocalOf<AppLayout> { error("layout not defined") }

@Composable
fun rememberAppLayout() = LocalAppLayout.current

@Composable
@Layout(".layout.RootLayout")
fun AppLayout(ctx: PageContext, content: @Composable () -> Unit) {
    val data = ctx.data.getValue<PageData>()

    val layout = remember(ctx.route.path) {
        AppLayout(
            fullscreen = mutableStateOf("fullscreen" in ctx.route.queryParams),
            pageStyle = mutableStateOf(data.style),
        )
    }

    Div({ classes("flex", "h-full", "w-full", "flex-col", "overflow-hidden", "select-none") }) {
        if (!layout.fullscreen) {
            NavBar(data.route)
        }

        Main({
            classes("min-h-0", "flex-1", "overflow-hidden")
            classes(layout.pageStyle.containerClasses)
        }) {
            Div({
                classes("mx-auto", "flex", "w-full", "flex-col", "h-full", "min-h-0", "overflow-hidden")
            }) {
                CompositionLocalProvider(LocalAppLayout provides layout) {
                    content()
                }
            }
        }

        if (!layout.fullscreen) {
            AppFooter()
        }
    }
}

@Composable
private fun NavBar(activePage: AppRoute?) {
    Header({
        classes(
            "shrink-0", "border-b", "border-slate-800/80", "bg-slate-950/95", "px-3", "py-2.5",
            "shadow-lg", "shadow-black/10", "md:px-6", "lg:px-8",
        )
    }) {
        Div({
            classes("mx-auto", "flex", "w-full", "items-center", "justify-between", "gap-3")
        }) {
            Anchor(AppRoute.LobbyList, {
                classes(
                    "group", "flex", "min-w-0", "items-center", "gap-2.5", "rounded-md", "px-1", "py-1",
                    "transition", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
                )
            }) {
                Span({
                    classes(
                        "grid", "size-8", "shrink-0", "place-items-center", "rounded-md", "border",
                        "border-emerald-500/40", "bg-emerald-500/15", "group-hover:border-emerald-400/50", "transition",
                    )
                }) {
                    Img(BasePath.prependTo("/favicon.png"), "HeXO", {
                        classes("size-full", "object-contain")
                    })
                }
                Span({ classes("truncate", "text-base", "font-bold", "text-xl", "text-slate-100", "hidden", "md:block") }) {
                    Text("MineKing's HeXO Tools")
                }
            }

            WatchPartyIndicator()

            Nav({
                classes(
                    "flex", "items-center", "gap-2", "rounded-xl", "border", "border-slate-800",
                    "bg-slate-900/70", "p-1", "shadow-inner", "shadow-black/20",
                )
            }) {
                NavBarEntry.entries.forEach {
                    NavLink(it.label, it.route, activePage?.navBarEntry == it)
                }
            }
        }
    }
}

@Composable
private fun WatchPartyIndicator() {
    val watchPartyController = rememberWatchPartyController()
    val watchParty = watchPartyController.currentWatchParty ?: return
    val connected by watchParty.connected.collectAsState()

    if (watchPartyController.hostWatchParty != null) {
        var open by remember { mutableStateOf(false) }

        SessionHostIndicatorButton(connected, onClick = { open = true })

        if (open) {
            Dialog(title = null, onClose = { open = false }) {
                WatchPartyHostOptions()
            }
        }
        return
    }

    SessionSubscriptionIndicator(connected)
}

@Composable
private fun SessionSubscriptionIndicator(connected: Boolean) {
    Div({
        classes(
            "group", "inline-flex", "min-w-0", "max-w-72", "items-center", "gap-2.5", "rounded-lg",
            "border", "border-sky-500/40", "bg-sky-500/10", "pr-2.5", "p-1.5", "text-left",
            "text-sky-100", "shadow-sm", "shadow-sky-950/20", "transition",
            "hover:border-sky-400/60", "hover:bg-sky-500/20", "hover:text-white",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-sky-400/60",
        )
    }) {
        WatchPartyIndicatorContent(WatchPartyIndicatorStyle.Subscriber, connected)
    }
}

@Composable
private fun SessionHostIndicatorButton(connected: Boolean, onClick: () -> Unit) {
    Button({
        classes(
            "group", "inline-flex", "min-w-0", "max-w-72", "items-center", "gap-2.5", "rounded-lg",
            "border", "border-emerald-500/40", "bg-emerald-500/10", "pr-2.5", "p-1.5", "text-left",
            "text-emerald-100", "shadow-sm", "shadow-emerald-950/20", "transition", "cursor-pointer",
            "hover:border-emerald-400/60", "hover:bg-emerald-500/20", "hover:text-white",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
        )
        attr("aria-label", "Open watch party host options")

        onClick { onClick() }
    }) {
        WatchPartyIndicatorContent(WatchPartyIndicatorStyle.Host, connected)

        ChevronRightIcon {
            classes("hidden", "size-4", "shrink-0", "text-emerald-300/70", "transition-transform", "group-hover:translate-x-0.5", "md:block")
        }
    }
}

private enum class WatchPartyIndicatorStyle(
    val iconClasses: List<String>,
    val statusClass: String,
    val spinnerClass: String,
    val titleClass: String,
    val subtitleClass: String,
    val title: String,
    val subtitle: String,
) {
    Host(
        iconClasses = listOf("border-emerald-400/40", "text-emerald-300", "group-hover:border-emerald-300/60", "group-hover:text-emerald-100"),
        statusClass = "bg-emerald-400",
        spinnerClass = "border-t-emerald-400!",
        titleClass = "text-emerald-100",
        subtitleClass = "text-emerald-300/70",
        title = "Hosting watch party",
        subtitle = "Host controls",
    ),
    Subscriber(
        iconClasses = listOf("border-sky-400/40", "text-sky-300", "group-hover:border-sky-300/60", "group-hover:text-sky-100"),
        statusClass = "bg-sky-300",
        spinnerClass = "border-t-sky-300!",
        titleClass = "text-sky-100",
        subtitleClass = "text-sky-300/70",
        title = "Watching watch party",
        subtitle = "Live sync enabled",
    ),
}

@Composable
private fun WatchPartyIndicatorContent(style: WatchPartyIndicatorStyle, connected: Boolean) {
    Span({
        classes(
            "relative", "grid", "size-8", "shrink-0", "place-items-center", "rounded-md", "border",
            "bg-slate-950/60", "transition",
        )

        classes(style.iconClasses)
    }) {
        Span({
            classes(
                "absolute", "-right-0.5", "-top-0.5", "size-2.5", "rounded-full", "border",
                "border-slate-950", style.statusClass,
            )
        })
        if (!connected) {
            LoadingIndicator { classes("size-4", "border-2!", style.spinnerClass) }
        } else {
            BroadcastIcon { classes("size-4", "shrink-0") }
        }
    }

    Span({ classes("hidden", "min-w-0", "flex-col", "leading-tight", "sm:flex") }) {
        Span({ classes("truncate", "text-xs", "font-bold", style.titleClass) }) {
            Text(if (!connected) "Reconnecting watch party" else style.title)
        }
        Span({ classes("truncate", "text-[11px]", "font-medium", style.subtitleClass) }) {
            Text(if (!connected) "Connection interrupted" else style.subtitle)
        }
    }
}

@Composable
private fun SettingsButton() {
    var dialogOpen by remember { mutableStateOf(false) }

    Button({
        attr("aria-label", "Open settings dialog")
        onClick { dialogOpen = true }
        classes(
            "inline-flex", "items-center", "gap-1.5", "rounded-md", "border", "border-slate-800",
            "bg-slate-900/60", "px-2.5", "py-1.5", "font-semibold", "text-slate-300", "transition",
            "hover:border-slate-600", "hover:bg-slate-800", "hover:text-slate-100",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60", "cursor-pointer",
        )
    }) {
        SettingsIcon {
            classes("size-4", "shrink-0")
        }
        Span({ classes("hidden", "sm:inline") }) {
            Text("Settings")
        }
    }

    if (dialogOpen) {
        Dialog("Settings", onClose = { dialogOpen = false }) {
            SettingsView()
        }
    }
}

@Composable
private fun AppFooter() {
    Div({
        classes(
            "shrink-0", "border-t", "border-slate-800/80", "bg-slate-950/95", "px-3", "py-2",
            "text-xs", "text-slate-500", "md:px-6", "lg:px-8",
        )
    }) {
        Div({
            classes("mx-auto", "flex", "w-full", "items-center", "justify-between", "gap-3")
        }) {
            Span {
                Text("Copyright © 2026 MineKing")
            }

            Span({ classes("flex", "gap-2") }) {
                SettingsButton()

                A(GITHUB_URL, {
                    target(ATarget.Blank)
                    attr("rel", "noreferrer noopener")
                    attr("aria-label", "Open GitHub repository")
                    classes(
                        "inline-flex", "items-center", "gap-1.5", "rounded-md", "border", "border-slate-800",
                        "bg-slate-900/60", "px-2.5", "py-1.5", "font-semibold", "text-slate-300", "transition",
                        "hover:border-slate-600", "hover:bg-slate-800", "hover:text-slate-100",
                        "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
                    )
                }) {
                    GitHubIcon {
                        classes("size-4", "shrink-0")
                    }
                    Span({ classes("hidden", "sm:inline") }) {
                        Text("GitHub")
                    }
                }
            }
        }
    }
}

@Composable
private fun NavLink(label: String, route: AppRoute, active: Boolean) {
    Anchor(route, {
        classes(
            "rounded-lg", "border", "px-3", "py-1.5", "text-sm", "font-semibold", "transition",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60", "md:px-4",
        )
        if (active) {
            classes("border-emerald-500/50", "bg-emerald-500/20", "text-emerald-200", "shadow-sm", "shadow-emerald-950/30")
        } else {
            classes("border-transparent", "text-slate-400", "hover:bg-slate-800", "hover:text-slate-100")
        }
    }) {
        Span({ classes("whitespace-nowrap") }) {
            Text(label)
        }
    }
}
