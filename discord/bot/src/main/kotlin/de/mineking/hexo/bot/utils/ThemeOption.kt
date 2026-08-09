package de.mineking.hexo.bot.utils

import de.mineking.discord.commands.OptionConfig
import de.mineking.discord.commands.choice
import de.mineking.discord.commands.map
import de.mineking.discord.commands.stringOption
import de.mineking.discord.commands.terminateCommand
import de.mineking.discord.localization.localize
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.userId
import de.mineking.hexo.discord.bot.config.CustomThemeId
import de.mineking.hexo.discord.bot.config.CustomThemeSelector
import de.mineking.hexo.utils.types.isSuccess
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

context(main: HeXODiscordBot)
fun OptionConfig.themeOption(name: String) = stringOption(name) {
    val customThemes = main.userThemeRepository
        ?.listUserThemes(user.userId)
        .orEmpty()

    replyChoices(
        customThemes.map { choice(it.id.value, it.name) } +
            DefaultTheme.entries.map { choice(it.name, it.name.localize()) },
    )
}.map { option ->
    if (option.isEmpty()) return@map main.getUserTheme(user.userId)
    option.value.parseTheme(terminate = ::terminateCommand)
}

context(main: HeXODiscordBot, callback: IReplyCallback)
suspend fun String.parseTheme(terminate: () -> Nothing): Theme {
    val defaultTheme = DefaultTheme.entries.find { it.name == this }
    if (defaultTheme != null) return defaultTheme.theme

    val result = main.userThemeRepository
        ?.getThemeById(CustomThemeSelector.Id(CustomThemeId(this)))
        ?: return DefaultTheme.HDS.theme

    if (!result.isSuccess()) {
        // TODO
        terminate()
    }

    return result.value
}
