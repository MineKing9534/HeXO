package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.AppGlobals
import com.varabyte.kobweb.core.isExporting
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.render.image.theme.BaseTheme
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.rgba
import org.w3c.dom.events.EventListener
import org.w3c.dom.url.URL
import kotlin.time.Duration

@Composable
fun <T> rememberPrevious(key: Any?, value: T): T? {
    var previous by remember(key) { mutableStateOf<T?>(null) }

    LaunchedEffect(value) {
        previous = value
    }

    return previous
}

@Composable
fun <T> rememberPrevious(value: T) = rememberPrevious(Unit, value)

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun <T> rememberAsyncResourceState(
    key: Any?,
    initialState: T,
    load: suspend () -> T,
    dispose: suspend (T) -> Unit,
): T {
    var state by remember(key) { mutableStateOf(initialState) }

    DisposableEffect(key) {
        var disposed = false

        state = initialState
        val job = GlobalScope.launch {
            val loadedResource = load()
            if (disposed) {
                dispose(loadedResource)
                return@launch
            }

            state = loadedResource
        }

        onDispose {
            disposed = true
            job.cancel()

            GlobalScope.launch { dispose(state) }
        }
    }

    return state
}

fun <T> MutableState<T>.interceptSet(handler: MutableState<T>.(T) -> Boolean) = object : MutableState<T> by this {
    override var value: T
        get() = this@interceptSet.value
        set(value) {
            if (handler(value)) {
                this@interceptSet.value = value
            }
        }
}

fun <T> MutableState<T>.onSet(handler: MutableState<T>.(T) -> Unit) = interceptSet {
    handler(it)
    true
}

@Composable
fun <T, U> State<T>.map(transform: (T) -> U): State<U> {
    val currentTransform = rememberUpdatedState(transform)
    return remember(this) {
        object : State<U> {
            override val value get() = currentTransform.value(this@map.value)
        }
    }
}

@Composable
fun <T, U> MutableState<T>.map(transform: (T) -> U, transformBack: (U) -> T): MutableState<U> {
    val currentTransform = rememberUpdatedState(transform)
    val currentTransformBack = rememberUpdatedState(transformBack)
    return remember(this) {
        object : MutableState<U> {
            override var value: U
                get() = currentTransform.value(this@map.value)
                set(value) {
                    this@map.value = currentTransformBack.value(value)
                }

            override fun component1() = value
            override fun component2(): (U) -> Unit = { value = it }
        }
    }
}

@Composable
fun rememberQueryParameter(
    name: String,
    initialValue: String? = null,
): MutableState<String?> {
    val state = remember(name, initialValue) { mutableStateOf(initialValue ?: currentQueryParameter(name)) }

    val value = state.value
    LaunchedEffect(name, value) {
        if (AppGlobals.isExporting) return@LaunchedEffect
        val url = URL(window.location.href)

        if (url.searchParams.get(name) == value) return@LaunchedEffect
        if (value == null) {
            url.searchParams.delete(name)
        } else {
            url.searchParams.set(name, value)
        }
        window.history.replaceState(null, "", url.toString())
    }

    DisposableEffect(name) {
        if (AppGlobals.isExporting) return@DisposableEffect onDispose {}
        val listener = EventListener { state.value = currentQueryParameter(name) }
        window.addEventListener("popstate", listener)

        onDispose { window.removeEventListener("popstate", listener) }
    }
    return state
}

private fun currentQueryParameter(name: String) = if (AppGlobals.isExporting) {
    null
} else {
    URL(window.location.href).searchParams.get(name)
}

@Composable
fun rememberTheme() = SettingsKey.Theme.collectAsState().map { it.theme }

private val Color.css get() = rgba(red, green, blue, alpha)
fun BaseTheme.playerCssColor(owner: CellOwner) = playerColor(owner).css
fun BaseTheme.playerColor(owner: CellOwner) = when (owner) {
    CellOwner.X -> playerXColor
    CellOwner.O -> playerOColor
}

fun Duration.formatCompact(): String {
    val hours = inWholeHours
    val minutes = inWholeMinutes % 60
    val seconds = inWholeSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun TimeControl.format() = when (this) {
    is TimeControl.Unlimited -> "Unlimited"
    is TimeControl.Turn -> "Turn $turnTime"
    is TimeControl.Match -> "Match $mainTime +$increment"
}
