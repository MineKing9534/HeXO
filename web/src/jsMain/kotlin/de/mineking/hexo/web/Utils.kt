package de.mineking.hexo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.mineking.hexo.board.render.image.theme.BaseTheme
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.core.CellOwner
import de.mineking.hexo.web.settings.SettingsKey
import de.mineking.hexo.web.settings.collectAsState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.rgba

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

fun <T, U> State<T>.map(transform: (T) -> U) = object : State<U> {
    override val value get() = transform(this@map.value)
}

@Composable
fun rememberTheme() = SettingsKey.Theme.collectAsState().map { it.theme }

private val Color.css get() = rgba(red.toInt(), green.toInt(), blue.toInt(), alpha.toInt())
fun BaseTheme.playerCssColor(owner: CellOwner) = playerColor(owner).css
fun BaseTheme.playerColor(owner: CellOwner) = when (owner) {
    CellOwner.X -> playerXColor
    CellOwner.O -> playerOColor
}
