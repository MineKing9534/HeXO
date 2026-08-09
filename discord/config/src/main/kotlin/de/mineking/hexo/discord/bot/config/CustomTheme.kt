package de.mineking.hexo.discord.bot.config

import de.mineking.hexo.board.render.image.RenderingContext
import de.mineking.hexo.board.render.image.theme.BaseTheme
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.utils.types.Omissible
import de.mineking.hexo.utils.types.isPresent
import de.mineking.hexo.utils.types.omitted
import de.mineking.hexo.utils.types.present
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Objects
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

@JvmInline
@Serializable
value class CustomThemeId(val value: String)

val Theme.base get() = DefaultTheme.entries.single { it.theme::class.isInstance(this) }

@Serializable
sealed class ThemeOverrideValue {
    abstract val value: Any?

    @Serializable
    @SerialName("color")
    data class ColorValue(override val value: Color) : ThemeOverrideValue()

    @Serializable
    @SerialName("double")
    data class DoubleValue(override val value: Double) : ThemeOverrideValue()
}

class CustomTheme(
    val id: CustomThemeId,
    val owner: DiscordUserId,
    val name: String,
    val base: DefaultTheme,
    val overrides: Map<String, ThemeOverrideValue>,
) : Theme() {
    val delegate = base.createCopy {
        if (it.name !in overrides) return@createCopy omitted<Any?>()
        overrides[it.name]?.value.present()
    }

    override val gap = delegate.gap
    override val backgroundColor = delegate.backgroundColor

    override fun render(context: RenderingContext, middleLayer: () -> Unit) = delegate.render(context, middleLayer)

    override fun equals(other: Any?) = other is CustomTheme && other.id == id
    override fun hashCode() = Objects.hash(id, owner, name)
}

private fun DefaultTheme.createCopy(mapper: (KParameter) -> Omissible<*>): BaseTheme {
    val constructor = theme::class.primaryConstructor!!
    return constructor.callBy(constructor.parameters.associateWith { param ->
        val value = mapper(param)
        if (value.isPresent()) return@associateWith value.value

        @Suppress("UNCHECKED_CAST")
        val property = theme::class.memberProperties
            .first { it.name == param.name }
            as KProperty1<BaseTheme, Any?>

        property.get(theme)
    })
}

sealed interface ThemeContainer {
    val theme: Theme

    data class Custom(override val theme: CustomTheme) : ThemeContainer
    data class Default(val default: DefaultTheme) : ThemeContainer {
        override val theme = default.theme
    }
}

fun ThemeContainer.toSelection() = when (this) {
    is ThemeContainer.Default -> UserThemeSelection.Default(default)
    is ThemeContainer.Custom -> UserThemeSelection.Custom(CustomThemeSelector.Id(theme.id))
}
