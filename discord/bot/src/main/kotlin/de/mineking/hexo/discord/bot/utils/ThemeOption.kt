package de.mineking.hexo.discord.bot.utils

import de.mineking.discord.LocalizedString
import de.mineking.discord.command.SlashCommandBuilder
import de.mineking.discord.command.option.CommandChoice
import de.mineking.discord.command.option.OptionRequirement
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.Action
import de.mineking.discord.menu.DeclarationScope
import de.mineking.discord.menu.components.StringSelectComponentScope
import de.mineking.discord.menu.components.stringSelect
import de.mineking.discord.menu.renderOrElse
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.IconEmojis
import de.mineking.hexo.discord.bot.ThemeEmojis
import de.mineking.hexo.discord.bot.config.CustomTheme
import de.mineking.hexo.discord.bot.config.CustomThemeId
import de.mineking.hexo.discord.bot.config.CustomThemeSelector
import de.mineking.hexo.discord.bot.config.ThemeContainer
import de.mineking.hexo.discord.bot.config.base
import de.mineking.hexo.discord.bot.userId
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun <T> SlashCommandBuilder.themeOption(
    name: String,
    description: LocalizedString,
    requirement: OptionRequirement<Theme, T>,
) = stringOption(name, description, requirement.mapInput {
    it.parseTheme(context.translationManager.bind(interaction.userLocale)).theme
}) {
    autocomplete {
        val localization = context.translationManager.bind<ThemeLocalization>(interaction.userLocale)

        val customThemes = context.main.userThemeRepository
            ?.listUserThemes(interaction.user.userId)
            .orEmpty()

        val custom = customThemes.map { CommandChoice(value = it.id.value, label = it.name) }
        val default = DefaultTheme.entries.map {
            CommandChoice(value = it.name, label = localization.defaultLabel(it))
        }

        custom + default
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private suspend fun String.parseTheme(localization: ThemeLocalization): ThemeContainer {
    val defaultTheme = DefaultTheme.entries.find { it.name == this }
    if (defaultTheme != null) return ThemeContainer.Default(defaultTheme)

    val result = context.main.userThemeRepository
        ?.getThemeById(CustomThemeSelector.Id(CustomThemeId(this)))
        ?: return ThemeContainer.Default(DefaultTheme.HDS)

    if (!result.isSuccess()) {
        localization.themeNotFound().throwValidationError(this)
    }

    return ThemeContainer.Custom(result.value)
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
suspend fun DeclarationScope.fetchUserThemeData() = renderOrElse(emptyList<CustomTheme>() to Theme.Default) {
    val user = requireInteraction().user.userId
    coroutineScope {
        val themes = async { context.main.userThemeRepository?.listUserThemes(user).orEmpty() }
        val default = async { context.main.getUserTheme(user) }

        themes.await() to default.await()
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
suspend fun <E : GenericInteractionCreateEvent> StringSelectComponentScope<E>.themeSelect(
    action: Action<E, Theme>,
    customThemes: List<CustomTheme>,
    selected: ThemeId?,
    current: ThemeId?,
) {
    val localization = translation<ThemeLocalization>()

    stringSelect(action.map { it.single().parseTheme(localization).theme }) {
        customThemes.forEach {
            option(
                value = it.id.value,
                label = it.name,
                default = it.themeId == selected,
                description = localization.customDescription(it.id.value),
                emoji = context.emojis[if (it.themeId == current) IconEmojis.Selected else IconEmojis.Paintbrush],
            )
        }

        DefaultTheme.entries.forEach {
            option(
                value = it.name,
                label = localization.defaultLabel(it),
                default = it.theme.themeId == selected,
                description = localization.defaultDescription(),
                emoji = if (it.theme.themeId == current) context.emojis[IconEmojis.Selected] else it.emoji,
            )
        }
    }
}

context(context: DiscordToolKitContext<*>)
val DefaultTheme.emoji get() = context.emojis[ThemeEmojis.valueOf(name)]

@Serializable
sealed interface ThemeId {
    @Serializable
    @SerialName("custom")
    data class Custom(val id: CustomThemeId) : ThemeId

    @Serializable
    @SerialName("default")
    data class Default(val theme: DefaultTheme) : ThemeId
}

val Theme.themeId get() = when (this) {
    is CustomTheme -> ThemeId.Custom(id)
    else -> ThemeId.Default(base)
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
suspend fun ThemeId.resolve() = when (this) {
    is ThemeId.Default -> theme.theme
    is ThemeId.Custom -> context.main.userThemeRepository
        ?.getThemeById(CustomThemeSelector.Id(id))
        ?.let { it as? Result.Success }
        ?.value
}

@LocalizationBundle("theme")
interface ThemeLocalization {
    fun defaultLabel(type: DefaultTheme): String
    fun defaultDescription(): String
    fun customDescription(id: String): String

    fun themeNotFound(): ErrorResponseMessage<String>
}
