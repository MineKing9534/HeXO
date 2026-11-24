package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.command.CommandRegistrationScope
import de.mineking.discord.command.ReplyMode
import de.mineking.discord.command.option.optional
import de.mineking.discord.command.option.required
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.extensions.reply
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.discord.menu.message.components.mediaGallery
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.throwValidationError
import de.mineking.hexo.discord.bot.userId
import de.mineking.hexo.discord.bot.utils.boardImage
import de.mineking.hexo.discord.bot.utils.themeOption

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun CommandRegistrationScope.renderHexoSlashCommand(boardMenu: MessageMenu<Board>) = localizedWith<RenderHexoSlashCommandLocalization> {
    slashCommand("hexo", localized { description() }) {
        val input = stringOption("input", localized { inputDescription() }, required())
        val theme = themeOption("theme", localized { themeDescription() }, optional { context.main.getUserTheme(interaction.user.userId) })
        val interactive = booleanOption("interactive", localized { interactiveDescription() }, optional(default = false))

        execute(ReplyMode.Defer(false)) {
            val board = try {
                context.main.notationParser.parse(input.value())
            } catch (e: HexoNotationException) {
                bind().notationError().throwValidationError(details = e.message)
            }

            if (interactive.value()) {
                replyMenu(boardMenu, board)
            } else {
                reply {
                    mediaGallery(boardImage(board, theme.value()))
                }
            }
        }
    }
}

@LocalizationBundle("slash", ".render")
internal interface RenderHexoSlashCommandLocalization {
    fun description(): String
    fun inputDescription(): String
    fun themeDescription(): String
    fun interactiveDescription(): String

    fun notationError(): ErrorResponseMessage<Unit>
}
