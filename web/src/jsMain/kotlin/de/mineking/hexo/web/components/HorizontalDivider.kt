package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Hr

@Composable
fun HorizontalDivider() {
    Hr {
        classes("my-3", "text-slate-600")
    }
}
