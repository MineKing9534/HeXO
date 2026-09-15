package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.requiredStringOption
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.replyMenu
import de.mineking.hexo.bot.menus.GameMenuParameter
import de.mineking.hexo.game.model.game.GameId
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

fun gameCommand(gameMenu: MessageMenu<GameMenuParameter, *>) = localizedSlashCommand<GameCommandLocalization>("game") {
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val id = requiredStringOption("id")

    execute {
        val id = id().split("/").last()

        deferReply().queue()
        replyMenu(gameMenu, GameMenuParameter(event, GameId(id), Int.MAX_VALUE)).queue()
    }
}

interface GameCommandLocalization : LocalizationFile
