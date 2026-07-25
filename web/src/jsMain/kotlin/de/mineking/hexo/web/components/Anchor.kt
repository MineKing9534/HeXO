package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.navigation.Anchor
import de.mineking.hexo.web.layout.AppRoute
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.w3c.dom.HTMLAnchorElement

@Composable
fun Anchor(
    route: AppRoute,
    attrs: AttrBuilderContext<HTMLAnchorElement>? = null,
    content: ContentBuilder<HTMLAnchorElement>? = null,
) {
    Anchor(route.href, attrs) {
        content?.invoke(this)
    }
}
