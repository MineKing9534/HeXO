package de.mineking.hexo.web.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.localStorage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class SettingsController {
    private val flows = mutableMapOf<SettingsKey<*>, MutableStateFlow<*>>()

    private fun <T> read(key: SettingsKey<T>): T {
        val value = localStorage.getItem(key.name) ?: return key.default

        @Suppress("UNCHECKED_CAST")
        return Json.decodeFromString(Json.serializersModule.serializer(key.type), value) as T
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun <T> StateFlow<T>.persist(key: SettingsKey<T>) {
        GlobalScope.launch {
            collect { value ->
                val encoded = Json.encodeToString(Json.serializersModule.serializer(key.type), value)
                localStorage.setItem(key.name, encoded)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: SettingsKey<T>) = flows.getOrPut(key) {
        MutableStateFlow(read(key))
            .also { it.persist(key) }
    } as MutableStateFlow<T>
}

private val LocalSettingsController = staticCompositionLocalOf<SettingsController> { error("SettingsController not initialized!") }

@Composable
fun SettingsControllerProvider(content: @Composable () -> Unit) {
    val settingsController = remember { SettingsController() }
    CompositionLocalProvider(LocalSettingsController provides settingsController) {
        content()
    }
}

@Composable
fun <T> rememberSettingsValue(key: SettingsKey<T>): MutableState<T> {
    val controller = LocalSettingsController.current
    val flow = remember(key) { controller[key] }
    val value by flow.collectAsState()

    return object : MutableState<T> {
        override var value: T
            get() = value
            set(value) {
                flow.value = value
            }

        override fun component1() = this.value
        override fun component2(): (T) -> Unit = { this.value = it }
    }
}
