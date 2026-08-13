package de.mineking.hexo.bot.utils

import de.mineking.discord.ui.IdGenerator
import de.mineking.discord.ui.MenuConfig
import de.mineking.discord.ui.currentLocalizationConfig
import de.mineking.discord.ui.modal.ModalComponent
import de.mineking.discord.ui.modal.ModalComponentBuilder
import de.mineking.discord.ui.modal.ModalContext
import net.dv8tion.jda.api.components.ModalTopLevelComponent
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal inline fun <reified T> MenuConfig<*, *>.bindLocalizationParameter(name: String, param: T) {
    val config = currentLocalizationConfig ?: return
    config.args += name to (param to typeOf<T>())
}

internal inline fun <C : ModalTopLevelComponent, reified T> ModalComponentBuilder<C, *>.bindLocalizationParameter(
    name: String,
    noinline param: () -> T,
) = bindLocalizationParameter(name, typeOf<T>(), param)

private fun <C : ModalTopLevelComponent> ModalComponentBuilder<C, *>.bindLocalizationParameter(name: String, type: KType, param: () -> Any?) {
    +object : ModalComponent<C, Nothing> {
        override suspend fun handle(context: ModalContext<*>) = error("Cannot happen")
        override suspend fun render(config: MenuConfig<*, *>, generator: IdGenerator): List<C> {
            config.currentLocalizationConfig?.apply {
                args[name] = param() to type
            }
            return emptyList()
        }
    }
}
