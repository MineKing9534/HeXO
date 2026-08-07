package de.mineking.hexo.web.settings

import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.sync.common.WatchPartyId
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class SettingsKey<out T> private constructor(val name: String, val type: KType, val default: T) {
    companion object {
        val keys: Map<String, SettingsKey<*>>
            field = mutableMapOf()

        private data class SettingsKeyData<out T>(val type: KType, val default: T)
        private inline fun <reified T> key(default: T) = SettingsKeyData(typeOf<T>(), default)

        private operator fun <T> SettingsKey<T>.getValue(thisRef: Companion, property: KProperty<*>) = this
        private operator fun <T> SettingsKeyData<T>.provideDelegate(thisRef: Companion, property: KProperty<*>): SettingsKey<T> {
            val name = property.name.replace("[a-z][A-Z]".toRegex()) {
                val (a, b) = it.value.toCharArray()
                "${a}_$b"
            }.lowercase()

            check(name !in keys) { "Duplicate settings key '$name'" }
            return SettingsKey(name, type, default)
                .also { keys[name] = it }
        }

        val SessionViewTimerSounds by key(true)
        val Volume by key(1f)
        val ReadOnlyBoardHoverIndicator by key(true)
        val SessionAnalyzer by key(true)
        val Theme by key(DefaultTheme.HDS)

        val HostWatchPartyId by key<WatchPartyId?>(null)
    }
}
