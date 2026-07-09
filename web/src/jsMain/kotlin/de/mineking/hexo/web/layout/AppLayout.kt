@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.web.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.core.data.getValue
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.navigation.Anchor
import com.varabyte.kobweb.navigation.BasePath
import de.mineking.hexo.hds.utils.EntityState
import de.mineking.hexo.web.components.Dialog
import de.mineking.hexo.web.icons.BroadcastIcon
import de.mineking.hexo.web.icons.ChevronRightIcon
import de.mineking.hexo.web.icons.GitHubIcon
import de.mineking.hexo.web.icons.SettingsIcon
import de.mineking.hexo.web.pages.watchparty.SessionHostOptions
import de.mineking.hexo.web.rememberWatchPartyController
import de.mineking.hexo.web.settings.SettingsView
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
    private val fullscreen: MutableState<Boolean>,
) {
    fun isFullscreen() = fullscreen.value

    fun setFullscreen(state: Boolean) {
        fullscreen.value = state
    }
}

private val LocalAppLayout = staticCompositionLocalOf<AppLayout> { error("layout not defined") }

@Composable
fun rememberAppLayout() = LocalAppLayout.current

@Composable
@Layout(".layout.RootLayout")
fun AppLayout(ctx: PageContext, content: @Composable () -> Unit) {
    val data = ctx.data.getValue<PageData>()

    val layout = remember(ctx.route.path) {
        AppLayout(fullscreen = mutableStateOf(false))
    }

    Div({ classes("flex", "h-full", "w-full", "flex-col", "overflow-hidden", "select-none") }) {
        if (!layout.isFullscreen()) {
            NavBar(data.route)
        }

        Main({
            classes("min-h-0", "flex-1", "overflow-hidden")
            classes(data.style.containerClasses)
        }) {
            Div({
                classes("mx-auto", "flex", "w-full", "flex-col", "h-full", "min-h-0", "overflow-hidden")
            }) {
                CompositionLocalProvider(LocalAppLayout provides layout) {
                    content()
                }
            }
        }

        if (!layout.isFullscreen()) {
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
            Anchor("/", {
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

            SessionSyncIndicator()

            Nav({
                classes(
                    "flex", "items-center", "gap-2", "rounded-xl", "border", "border-slate-800",
                    "bg-slate-900/70", "p-1", "shadow-inner", "shadow-black/20",
                )
            }) {
                NavBarEntry.entries.forEach {
                    NavLink(it.label, it.route.href, activePage?.navBarEntry == it)
                }
            }
        }
    }
}

@Composable
private fun SessionSyncIndicator() {
    val watchPartyController = rememberWatchPartyController()

    if (watchPartyController.hostWatchParty != null) {
        var open by remember { mutableStateOf(false) }

        SessionHostIndicatorButton(onClick = { open = true })

        if (open) {
            Dialog(title = null, onClose = { open = false }) {
                SessionHostOptions()
            }
        }
        return
    }

    if (watchPartyController.subscribedWatchParty is EntityState.Data) SessionSubscriptionIndicator()
}

@Composable
private fun SessionSubscriptionIndicator() {
    Div({
        classes(
            "group", "inline-flex", "min-w-0", "max-w-72", "items-center", "gap-2.5", "rounded-lg",
            "border", "border-sky-500/40", "bg-sky-500/10", "pr-2.5", "p-1.5", "text-left",
            "text-sky-100", "shadow-sm", "shadow-sky-950/20", "transition",
            "hover:border-sky-400/60", "hover:bg-sky-500/20", "hover:text-white",
            "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-sky-400/60",
        )
    }) {
        Span({
            classes(
                "relative", "grid", "size-8", "shrink-0", "place-items-center", "rounded-md", "border",
                "border-sky-400/40", "bg-slate-950/60", "text-sky-300", "transition",
                "group-hover:border-sky-300/60", "group-hover:text-sky-100",
            )
        }) {
            Span({
                classes("absolute", "-right-0.5", "-top-0.5", "size-2.5", "rounded-full", "border", "border-slate-950", "bg-sky-300")
            })
            BroadcastIcon {
                classes("size-4", "shrink-0")
            }
        }

        Span({ classes("hidden", "min-w-0", "flex-col", "leading-tight", "sm:flex") }) {
            Span({ classes("truncate", "text-xs", "font-bold", "text-sky-100") }) {
                Text("Watching watch party")
            }
            Span({ classes("truncate", "text-[11px]", "font-medium", "text-sky-300/70") }) {
                Text("Live sync enabled")
            }
        }
    }
}

@Composable
private fun SessionHostIndicatorButton(onClick: () -> Unit) {
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
        Span({
            classes(
                "relative", "grid", "size-8", "shrink-0", "place-items-center", "rounded-md", "border",
                "border-emerald-400/40", "bg-slate-950/60", "text-emerald-300", "transition",
                "group-hover:border-emerald-300/60", "group-hover:text-emerald-100",
            )
        }) {
            Span({
                classes("absolute", "-right-0.5", "-top-0.5", "size-2.5", "rounded-full", "border", "border-slate-950", "bg-emerald-400")
            })
            BroadcastIcon {
                classes("size-4", "shrink-0")
            }
        }

        Span({ classes("hidden", "min-w-0", "flex-col", "leading-tight", "sm:flex") }) {
            Span({ classes("truncate", "text-xs", "font-bold", "text-emerald-100") }) {
                Text("Hosting watch party")
            }
            Span({ classes("truncate", "text-[11px]", "font-medium", "text-emerald-300/70") }) {
                Text("Host controls")
            }
        }

        ChevronRightIcon {
            classes("hidden", "size-4", "shrink-0", "text-emerald-300/70", "transition-transform", "group-hover:translate-x-0.5", "md:block")
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
                    attr("target", "_blank")
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
private fun NavLink(label: String, href: String, active: Boolean) {
    Anchor(href, {
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
