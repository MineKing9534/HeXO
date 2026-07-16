package de.mineking.hexo.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.web.icons.CheckIcon
import de.mineking.hexo.web.icons.CopyIcon
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.Button
import kotlin.time.Duration.Companion.seconds

@Composable
fun CopyButton(
    value: String,
    label: String = "value",
    rightClass: String = "right-2",
    verticalClasses: List<String> = listOf("top-1/2", "-translate-y-1/2"),
) {
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied, value) {
        if (!copied) return@LaunchedEffect
        delay(1.5.seconds)
        copied = false
    }

    Button({
        attr("aria-label", if (copied) "$label copied" else "Copy $label")
        attr("title", if (copied) "Copied" else "Copy $label")
        classes(
            "absolute", rightClass, "grid", "size-8", "place-items-center", "rounded-md", "transition",
            "cursor-pointer", "focus:outline-none",
            "focus-visible:ring-2", "focus-visible:ring-emerald-400/60",
        )
        classes(verticalClasses)
        if (copied) {
            classes("text-emerald-300")
        } else {
            classes("text-slate-400", "hover:bg-slate-800", "hover:text-slate-100")
        }
        onClick {
            window.navigator.clipboard.writeText(value)
            copied = true
        }
    }) {
        if (copied) {
            CheckIcon { classes("size-4") }
        } else {
            CopyIcon { classes("size-4") }
        }
    }
}
