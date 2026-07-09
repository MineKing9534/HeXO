package de.mineking.hexo.web.settings

import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class SettingsKey<out T> private constructor(val name: String, val type: KType, val default: T) {
    companion object {
        private data class SettingsKeyData<out T>(val type: KType, val default: T) {
            private var value: SettingsKey<T>? = null

            private fun String.transformName() = replace("[a-z][A-Z]".toRegex()) {
                val (a, b) = it.value.toCharArray()
                "${a}_$b"
            }.lowercase()

            operator fun getValue(thisRef: Companion, property: KProperty<*>) = value
                ?: SettingsKey(property.name.transformName(), type, default)
                    .also { value = it }
        }
        private inline fun <reified T> key(default: T) = SettingsKeyData(typeOf<T>(), default)

        val SessionViewTimerSounds by key(true)
        val ReadOnlyBoardHoverIndicator by key(true)
    }
}
