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
import de.mineking.hexo.web.onSet
import de.mineking.hexo.web.pages.watchparty.WatchPartyHostOptions
import de.mineking.hexo.web.rememberWatchPartyController
import de.mineking.hexo.web.settings.SettingsView
import kotlinx.browser.window
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
import org.w3c.dom.url.URL

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
    supportsFullScreen: MutableState<Boolean>,
    pageStyle: MutableState<PageStyle>,
) {
    var fullscreen by fullscreen
    var supportsFullScreen by supportsFullScreen
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
            fullscreen = mutableStateOf("fullscreen" in ctx.route.queryParams).onSet { fullscreen ->
                val url = URL(window.location.href)
                if (fullscreen) {
                    url.searchParams.set("fullscreen", "true")
                } else {
                    url.searchParams.delete("fullscreen")
                }
                window.history.replaceState(null, "", url.toString())
            },
            supportsFullScreen = mutableStateOf(false),
            pageStyle = mutableStateOf(data.style),
        )
    }

    val fullscreen = layout.fullscreen && layout.supportsFullScreen

    Div({ classes("flex", "h-full", "w-full", "flex-col", "overflow-hidden", "select-none") }) {
        if (!fullscreen) {
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

        if (!fullscreen) {
            AppFooter()
        }
    }
}

@Composable
private fun NavBar(activePage: AppRoute?) {
    Header({
        classes(
            "relative", "z-20", "shrink-0", "border-b", "border-white/8", "bg-slate-950/95", "px-3", "py-2.5",
            "shadow-xl", "shadow-black/15", "md:px-6", "lg:px-8",
        )
    }) {
        Div({
            classes(
                "mx-auto", "grid", "w-full", "grid-cols-[1fr_auto]", "items-center", "gap-x-3", "gap-y-2",
                "sm:grid-cols-[1fr_auto_1fr]",
            )
        }) {
            Anchor(AppRoute.SessionList, {
                classes(
                    "group", "flex", "min-w-0", "justify-self-start", "items-center", "gap-2.5", "rounded-lg", "px-1", "py-1",
                    "transition", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
                )
            }) {
                Span({
                    classes(
                        "grid", "size-9", "shrink-0", "place-items-center", "rounded-lg", "border",
                        "border-emerald-400/30", "bg-emerald-400/10", "shadow-lg", "shadow-emerald-950/30",
                        "transition", "group-hover:border-emerald-300/50", "group-hover:bg-emerald-400/15",
                    )
                }) {
                    Img(BasePath.prependTo("/favicon.png"), "HeXO", {
                        classes("size-full", "object-contain", "p-0.5")
                    })
                }
                Span({ classes("hidden", "min-w-0", "flex-col", "leading-none", "md:flex") }) {
                    Span({ classes("text-lg", "font-black", "tracking-tight", "text-slate-50") }) { Text("HeXO") }
                    Span({ classes("mt-1", "text-[10px]", "font-semibold", "uppercase", "tracking-[0.18em]", "text-emerald-300/70") }) {
                        Text("Play · Analyse · Connect")
                    }
                }
            }

            Nav({
                classes(
                    "col-span-2", "row-start-2", "flex", "justify-self-center", "items-center", "gap-1.5", "rounded-xl",
                    "border", "border-slate-700/60", "bg-slate-900/85", "p-1.5", "shadow-xl", "shadow-black/25",
                    "sm:col-span-1", "sm:col-start-2", "sm:row-start-1",
                )
            }) {
                NavBarEntry.entries.forEach {
                    NavLink(it.label, it.route, activePage?.navBarEntry == it)
                }
            }

            Div({ classes("col-start-2", "row-start-1", "flex", "min-w-0", "justify-self-end", "sm:col-start-3") }) {
                WatchPartyIndicator()
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
            "inline-flex", "items-center", "rounded-lg", "border", "px-3", "py-2", "text-[11px]", "font-bold",
            "uppercase", "tracking-wider", "transition-all", "focus:outline-none", "focus-visible:ring-2",
            "focus-visible:ring-emerald-400/60", "sm:px-3", "md:px-4", "md:text-xs",
        )
        if (active) {
            classes(
                "border-white/10", "bg-slate-800", "text-slate-50", "shadow-md", "shadow-black/20",
            )
        } else {
            classes("border-transparent", "text-slate-500", "hover:bg-slate-800/60", "hover:text-slate-200")
        }
    }) {
        Span({ classes("whitespace-nowrap") }) {
            Text(label)
        }
    }
}
