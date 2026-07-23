package de.mineking.hexo.web.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.w3c.dom.Storage
import org.w3c.dom.StorageEvent
import org.w3c.dom.events.EventListener

interface SettingsController {
    operator fun <T> get(key: SettingsKey<T>): MutableStateFlow<T>
}

private class SettingsControllerImpl(private val storage: Storage) : SettingsController {
    private val flows = mutableMapOf<SettingsKey<*>, MutableStateFlow<*>>()

    private fun <T> read(key: SettingsKey<T>): T {
        val value = storage.getItem(key.name) ?: return key.default

        @Suppress("UNCHECKED_CAST")
        return Json.decodeFromString(Json.serializersModule.serializer(key.type), value) as T
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun <T> StateFlow<T>.persist(key: SettingsKey<T>) {
        GlobalScope.launch {
            collect { value ->
                if (value == key.default) {
                    if (storage.getItem(key.name) == null) return@collect

                    storage.removeItem(key.name)
                    println("Removed settings key '${key.name}'")
                } else {
                    val encoded = Json.encodeToString(Json.serializersModule.serializer(key.type), value)
                    if (encoded == storage.getItem(key.name)) return@collect

                    storage.setItem(key.name, encoded)
                    println("Updated settings key '${key.name}' to '$encoded'")
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> reload(key: SettingsKey<T>) {
        val flow = flows[key] as? MutableStateFlow<T>? ?: return
        val value = read(key)
        flow.value = value
    }

    @Suppress("UNCHECKED_CAST")
    override operator fun <T> get(key: SettingsKey<T>) = flows.getOrPut(key) {
        MutableStateFlow(read(key))
            .also { it.persist(key) }
    } as MutableStateFlow<T>
}

private val LocalSettingsController = staticCompositionLocalOf<SettingsController> { error("SettingsController not initialized!") }

@Composable
fun SettingsControllerProvider(storage: Storage, content: @Composable (SettingsController) -> Unit) {
    val settingsController = remember { SettingsControllerImpl(storage) }

    DisposableEffect(Unit) {
        val listener = EventListener { event ->
            check(event is StorageEvent)
            if (event.storageArea != storage || event.key == null) return@EventListener

            val key = SettingsKey.keys[event.key]
            if (key == null) {
                println("Received storage event for unknown key '${event.key}'")
                return@EventListener
            }

            settingsController.reload(key)
        }

        window.addEventListener("storage", listener)
        onDispose { window.removeEventListener("storage", listener) }
    }

    CompositionLocalProvider(LocalSettingsController provides settingsController) {
        content(settingsController)
    }
}

@Composable
fun <T> SettingsKey<T>.collectAsState(): MutableState<T> {
    val controller = LocalSettingsController.current
    val flow = remember(this) { controller[this] }
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
