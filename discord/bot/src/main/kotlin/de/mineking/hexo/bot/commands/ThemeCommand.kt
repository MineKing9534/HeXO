package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.localizedMenuCommand
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.actionRow
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.mediaGallery
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.board.parse.parseRectilinearNotation
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.MessageColor
import de.mineking.hexo.bot.utils.asMediaGalleryItem
import de.mineking.hexo.bot.utils.effectiveLocale
import de.mineking.hexo.bot.utils.fetchUserThemeData
import de.mineking.hexo.bot.utils.parseTheme
import de.mineking.hexo.bot.utils.respond
import de.mineking.hexo.bot.utils.themeSelect
import de.mineking.hexo.discord.bot.config.toSelection
import de.mineking.hexo.utils.types.isSuccess
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

private val DEMO_BOARD = "4(p4o)-(q4x)/3x(!)o(!)/3(!).(x).(o)/3xo/.(>5)".parseRectilinearNotation()

context(main: HeXODiscordBot)
fun themeCommand() = localizedMenuCommand<ThemeCommandLocalization>("theme") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val locale = parameter({ DiscordLocale.UNKNOWN }, { it.effectiveLocale }, { effectiveLocale })
    localize(locale)

    val (themes, current) = fetchUserThemeData()

    +container {
        +localizedTextDisplay("header")
        +separator(spacing = Separator.Spacing.LARGE)

        +mediaGallery(DEMO_BOARD.asMediaGalleryItem(current))

        +separator(spacing = Separator.Spacing.LARGE)
        +actionRow(themeSelect(themes to current) {
            deferEdit().queue()

            val theme = event.values.single().parseTheme(terminate = ::terminateRender)
            val result = main.userThemeRepository?.setCurrentUserTheme(user.userId, theme.toSelection()) ?: return@themeSelect

            if (!result.isSuccess()) {
                respond(MessageColor.Error, localization.responseErrorThemeChange(userLocale))
                terminateRender()
            }

            forceUpdate()
        })
    }
}

interface ThemeCommandLocalization : LocalizationFile {
    @Localize
    fun responseErrorThemeChange(@Locale locale: DiscordLocale): String
}
