package de.mineking.hexo.web.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import de.mineking.hexo.web.components.AppLayout
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Page
@Composable
fun NotFoundPage() {
    AppLayout(activePage = null) {
        Div({ classes("grid", "flex-1", "w-full", "place-items-center") }) {
            Div({
                classes(
                    "grid", "w-full", "max-w-xl", "place-items-center", "rounded-2xl", "border", "border-slate-800",
                    "bg-linear-to-br", "from-slate-900", "to-slate-900/30", "px-5", "py-10", "shadow-2xl", "shadow-black/30",
                )
            }) {
                Div({ classes("flex", "max-w-full", "flex-col", "items-center", "justify-center", "gap-5") }) {
                    Section({ classes("flex", "flex-col", "items-center", "gap-2") }) {
                        H1({ classes("text-xl", "font-bold", "text-slate-100") }) {
                            Span({
                                classes(
                                    "inline-grid", "h-8", "w-16", "place-items-center", "rounded-full", "border",
                                    "border-rose-400", "bg-rose-500/15", "font-bold", "text-rose-400",
                                    "shadow-lg", "mr-4",
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
                }
            }
        }
    }
}
