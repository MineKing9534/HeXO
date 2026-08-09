package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.requiredStringOption
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.utils.asMediaGalleryItem
import de.mineking.hexo.bot.utils.finalErrorResponse
import de.mineking.hexo.bot.utils.themeOption
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder

context(main: HeXODiscordBot)
fun renderHexoSlashCommand() = localizedSlashCommand<RenderHexoCommandLocalization>("hexo") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val input = requiredStringOption("input")
    val theme = themeOption("theme")

    execute {
        val input = input()
        val theme = theme()

        deferReply().queue()
        val board = try {
            main.notationParser.parse(input)
        } catch (e: HexoNotationException) {
            finalErrorResponse(localization.responseError(userLocale, input, e.message))
        }

        hook.editOriginal(
            MessageEditBuilder()
                .setReplace(true)
                .setComponents(MediaGallery.of(main.run { board.asMediaGalleryItem(theme) }))
                .build(),
        ).queue()
    }
}

interface RenderHexoCommandLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale, @LocalizationParameter input: String, @LocalizationParameter message: String?): String
}
