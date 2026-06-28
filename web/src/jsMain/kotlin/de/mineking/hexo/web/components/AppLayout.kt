@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.navigation.Anchor
import com.varabyte.kobweb.navigation.BasePath
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg

enum class AppPage {
    Home,
    Sandbox,
}

private const val GITHUB_URL = "https://github.com/MineKing9534/HeXO-Renderer"

@Composable
fun AppLayout(
    activePage: AppPage?,
    scrollContent: Boolean = true,
    constrainContent: Boolean = true,
    content: @Composable () -> Unit,
) {
    Div({ classes("flex", "h-full", "w-full", "flex-col", "overflow-hidden", "select-none") }) {
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
                    Span({ classes("truncate", "text-base", "font-bold", "text-xl", "text-slate-100") }) {
                        Text("MineKing's HeXO Tools")
                    }
                }

                Nav({
                    classes(
                        "flex", "items-center", "gap-2", "rounded-xl", "border", "border-slate-800",
                        "bg-slate-900/70", "p-1", "shadow-inner", "shadow-black/20",
                    )
                }) {
                    NavLink("Lobbies", "/", activePage == AppPage.Home)
                    NavLink("Sandbox", "/sandbox", activePage == AppPage.Sandbox)
                }
            }
        }

        Main({
            classes("min-h-0", "flex-1")
            if (scrollContent) {
                classes("overflow-y-auto", "p-3", "md:p-6")
            } else {
                classes("overflow-hidden")
            }
        }) {
            Div({
                classes("mx-auto", "flex", "min-h-full", "w-full", "flex-col")
                if (constrainContent) classes("max-w-5xl", "gap-6")
            }) {
                content()
            }
        }

        AppFooter()
    }
}

@OptIn(ExperimentalComposeWebSvgApi::class)
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
                Svg("0 0 16 16", {
                    attr("aria-hidden", "true")
                    attr("fill", "currentColor")
                    classes("size-4", "shrink-0")
                }) {
                    Path(
                        "M8 1.2a6.8 6.8 0 0 0-2.15 13.25c.34.06.46-.15.46-.33v-1.2c-1.9.42-2.3-.81-2.3-.81-.31-.8-.76-1.01-.76-1.01-.62-.43.05-.42.05-.42.69.05 1.05.72 1.05.72.61 1.07 1.61.76 2 .58.06-.45.24-.76.44-.93-1.52-.18-3.12-.78-3.12-3.45 0-.76.27-1.38.7-1.87-.07-.18-.31-.9.07-1.84 0 0 .58-.19 1.88.72A6.37 6.37 0 0 1 8 4.38c.58 0 1.15.08 1.69.24 1.3-.91 1.87-.72 1.87-.72.38.94.14 1.66.07 1.84.44.49.7 1.11.7 1.87 0 2.68-1.6 3.27-3.13 3.44.25.22.47.65.47 1.32v1.95c0 .18.12.39.47.32A6.8 6.8 0 0 0 8 1.2Z",
                    )
                }
                Span({ classes("hidden", "sm:inline") }) {
                    Text("GitHub")
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
