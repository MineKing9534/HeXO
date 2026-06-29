package de.mineking.hexo.web.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import de.mineking.hexo.web.components.AppLayout
import de.mineking.hexo.web.components.Badge
import de.mineking.hexo.web.components.Color
import de.mineking.hexo.web.components.StatusCard
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Page
@Composable
fun NotFoundPage() {
    AppLayout(activePage = null) {
        StatusCard {
            H1({ classes("text-xl", "font-bold", "text-slate-100") }) {
                Badge(Color.Rose, {
                    classes("mr-4", "h-8", "w-16", "justify-center", "font-extrabold!", "shadow-lg", "text-xl")
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
