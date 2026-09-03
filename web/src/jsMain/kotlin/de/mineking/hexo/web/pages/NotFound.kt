package de.mineking.hexo.web.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import de.mineking.hexo.web.components.NotFoundCard
import de.mineking.hexo.web.layout.PageData

@InitRoute
fun initNotFoundPage(ctx: InitRouteContext) {
    ctx.data.add(PageData(null))
}

@Page
@Composable
fun NotFoundPage() {
    NotFoundCard(
        title = "Page not found",
        description = "The requested page does not exist or may have moved.",
        eyebrow = "404 error",
    )
}
