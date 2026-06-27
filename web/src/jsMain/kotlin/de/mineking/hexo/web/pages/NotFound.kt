package de.mineking.hexo.web.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.navigation.BasePath
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Page
@Composable
fun NotFoundPage() {
    Div({ classes("grid", "h-full", "w-full", "place-items-center", "p-3", "md:p-6") }) {
        Div({
            classes(
                "grid", "w-full", "max-w-xl", "place-items-center", "rounded-2xl", "border", "border-slate-800",
                "bg-linear-to-br", "from-slate-900", "to-slate-900/30", "p-5", "shadow-2xl", "shadow-black/30",
            )
        }) {
            Div({ classes("flex", "max-w-full", "flex-col", "items-center", "justify-center", "gap-5") }) {
                Section({ classes("flex", "flex-col", "items-center", "gap-2") }) {
                    H1({ classes("text-xl", "font-bold", "text-slate-100") }) {
                        Span({
                            classes(
                                "inline-grid", "h-8", "w-16", "place-items-center", "rounded-full", "border", "border-rose-400",
                                "bg-rose-500/15", "font-bold", "text-rose-400", "shadow-lg", "mr-4",
                            )
                        }) {
                            Text("404")
                        }
                        Text("Page Not Found")
                    }
                    P({ classes("max-w-full", "text-sm", "leading-relaxed", "text-slate-400") }) {
                        Text("The requested page does not exist or has moved.")
                    }
                }

                Div({ classes("mt-4", "flex", "flex-col", "items-center", "justify-center", "gap-2", "md:flex-row") }) {
                    A(BasePath.prependTo("/"), {
                        classes(
                            "rounded-lg", "border", "border-emerald-500/40", "bg-emerald-500/15", "px-4", "py-2",
                            "text-sm", "font-semibold", "text-emerald-300", "shadow-lg", "transition",
                            "hover:bg-emerald-500/25", "hover:text-emerald-100",
                        )
                    }) {
                        Text("Go home")
                    }
                    A(BasePath.prependTo("/sandbox"), {
                        classes(
                            "rounded-lg", "border", "border-slate-700", "bg-slate-950/60", "px-4", "py-2",
                            "text-sm", "font-semibold", "text-slate-300", "transition", "hover:bg-slate-800",
                            "hover:text-slate-100",
                        )
                    }) {
                        Text("Open sandbox")
                    }
                }
            }
        }
    }
}
