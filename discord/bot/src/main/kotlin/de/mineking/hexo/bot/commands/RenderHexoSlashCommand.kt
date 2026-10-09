package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.option
import de.mineking.discord.commands.orElse
import de.mineking.discord.commands.requiredStringOption
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.replyMenu
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.menus.InteractiveBoardMenuParameter
import de.mineking.hexo.bot.utils.asMediaGalleryItem
import de.mineking.hexo.bot.utils.finalErrorResponse
import de.mineking.hexo.bot.utils.themeOption
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder

context(main: HeXODiscordBot)
fun renderHexoSlashCommand(
    boardMenu: MessageMenu<InteractiveBoardMenuParameter, *>,
) = localizedSlashCommand<RenderHexoCommandLocalization>("hexo") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val input = requiredStringOption("input")
    val theme = themeOption("theme")
    val interactive = option<Boolean>("interactive").orElse(false)

    execute {
        deferReply().queue()

        val input = input()
        val theme = theme()
        val interactive = interactive()

        val board = try {
            main.notationParser.parse(input)
        } catch (e: HexoNotationException) {
            finalErrorResponse(localization.responseError(userLocale, input, e.message))
        }

        if (interactive) {
            replyMenu(boardMenu, InteractiveBoardMenuParameter(board, event)).queue()
        } else {
            hook.editOriginal(
                MessageEditBuilder()
                    .setReplace(true)
                    .setComponents(MediaGallery.of(main.run { board.asMediaGalleryItem(userLocale, theme) }))
                    .build(),
            ).queue()
        }
    }
}

interface RenderHexoCommandLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale, @LocalizationParameter input: String, @LocalizationParameter message: String?): String
}
