package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.command.ReplyMode
import de.mineking.discord.command.RootCommandRegistrationScope
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.extensions.reply
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.userId
import de.mineking.hexo.discord.bot.utils.compositeHexoNotation

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun RootCommandRegistrationScope.renderHexoMessageCommand() = localizedWith<RenderHexoMessageCommandLocalization> {
    messageCommand(localized { name() }) {
        execute(ReplyMode.Defer(false)) {
            val theme = context.main.getUserTheme(interaction.user.userId)
            val content = interaction.target.contentRaw

            reply {
                compositeHexoNotation(content, theme)
            }
        }
    }
}

@LocalizationBundle("context", ".render")
internal interface RenderHexoMessageCommandLocalization {
    fun name(): String
}
