package de.mineking.hexo.bot.utils

import de.mineking.discord.commands.OptionConfig
import de.mineking.discord.commands.choice
import de.mineking.discord.commands.map
import de.mineking.discord.commands.stringOption
import de.mineking.discord.commands.terminateCommand
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.localization.localize
import de.mineking.discord.ui.MenuConfig
import de.mineking.discord.ui.SharedElement
import de.mineking.discord.ui.builder.components.StringSelectHandler
import de.mineking.discord.ui.builder.components.selectOption
import de.mineking.discord.ui.builder.components.stringSelect
import de.mineking.discord.ui.currentLocalizationConfig
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.renderValue
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.bot.CustomEmoji
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.localization
import de.mineking.hexo.bot.userId
import de.mineking.hexo.discord.bot.config.CustomTheme
import de.mineking.hexo.discord.bot.config.CustomThemeId
import de.mineking.hexo.discord.bot.config.CustomThemeSelector
import de.mineking.hexo.discord.bot.config.ThemeContainer
import de.mineking.hexo.utils.types.isSuccess
import dev.freya02.jda.emojis.unicode.Emojis
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

val EMOJI_THEME_SELECTED = Emojis.WHITE_CHECK_MARK

context(main: HeXODiscordBot)
fun OptionConfig.themeOption(name: String) = stringOption(name) {
    val customThemes = main.userThemeRepository
        ?.listUserThemes(user.userId)
        .orEmpty()

    replyChoices(
        customThemes.map { choice(it.id.value, it.name) } +
            DefaultTheme.entries.map {
                choice(it.name, "theme.${it.name}.label".localize(), main.localization<ThemeLocalization>())
            },
    )
}.map { option ->
    if (option.isEmpty()) return@map main.getUserTheme(user.userId)
    option.value.parseTheme(terminate = ::terminateCommand).theme
}

context(main: HeXODiscordBot, callback: IReplyCallback)
suspend fun String.parseTheme(terminate: () -> Nothing): ThemeContainer {
    val defaultTheme = DefaultTheme.entries.find { it.name == this }
    if (defaultTheme != null) return ThemeContainer.Default(defaultTheme)

    val result = main.userThemeRepository
        ?.getThemeById(CustomThemeSelector.Id(CustomThemeId(this)))
        ?: return ThemeContainer.Default(DefaultTheme.HDS)

    if (!result.isSuccess()) {
        callback.respond(MessageColor.Error, main.localization<ThemeLocalization>().themeErrorNotFound(callback.userLocale, this), forceNew = true)
        terminate()
    }

    return ThemeContainer.Custom(result.value)
}

context(main: HeXODiscordBot)
suspend fun MenuConfig<out Interaction, *>.fetchUserThemeData() = renderValue(emptyList<CustomTheme>() to Theme.Default) {
    val user = parameter({ error("") }, { it.user.userId }, { user.userId })
    coroutineScope {
        val themes = async { main.userThemeRepository?.listUserThemes(user).orEmpty() }
        val default = async { main.getUserTheme(user) }

        themes.await() to default.await()
    }
}

context(main: HeXODiscordBot)
fun MenuConfig<out Interaction, *>.themeSelect(
    name: String,
    customThemes: List<CustomTheme>,
    isSelected: (Theme) -> Boolean,
    isCurrent: (Theme) -> Boolean,
    handler: StringSelectHandler? = null,
): SharedElement<StringSelectMenu, StringSelectInteractionEvent, List<String>> {
    val options = renderValue {
        val localization = main.localization<ThemeLocalization>()

        val customOptions = customThemes.map {
            selectOption(
                value = it.id.value,
                label = it.name,
                default = isSelected(it),
                description = localization.themeCustomDescription(currentLocalizationConfig!!.locale, it.id.value),
                emoji = if (isCurrent(it)) EMOJI_THEME_SELECTED else Emojis.ART,
            )
        }

        val defaultOptions = DefaultTheme.entries.map {
            selectOption(
                value = it.name,
                default = isSelected(it.theme),
                label = "theme.${it.name}.label".localize(),
                description = "theme.default.description".localize(),
                localization = main.localization<ThemeLocalization>(),
                emoji = if (isCurrent(it.theme)) {
                    EMOJI_THEME_SELECTED
                } else {
                    when (it) {
                        DefaultTheme.HDS -> main.emojiManager[CustomEmoji.ThemeHDS]
                        DefaultTheme.HTTTX -> main.emojiManager[CustomEmoji.ThemeHTTTX]
                        DefaultTheme.Tyto -> main.emojiManager[CustomEmoji.ThemeTyto]
                    }
                },
            )
        }

        customOptions + defaultOptions
    }

    return stringSelect(
        name = name,
        placeholder = null,
        options = options.orEmpty(),
        handler = handler,
    )
}

interface ThemeLocalization : LocalizationFile {
    @Localize
    fun themeCustomDescription(@Locale locale: DiscordLocale, @LocalizationParameter id: String): String

    @Localize
    fun themeErrorNotFound(@Locale locale: DiscordLocale, @LocalizationParameter id: String): String
}
