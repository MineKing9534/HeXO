package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.localizedMessageCommand
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.replyRichHexoNotation
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

context(main: HeXODiscordBot)
fun renderHexoMessageCommand() = localizedMessageCommand<RenderHexoCommandLocalization>("renderMessage") {
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    execute {
        deferReply().queue()

        val theme = main.getUserTheme(user.userId)
        replyRichHexoNotation(target.contentRaw, theme)
    }
}
