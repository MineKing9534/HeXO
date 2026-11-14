package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.command.CommandRegistrationScope
import de.mineking.discord.command.ReplyMode
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.message.MessageMenu

context(_: DiscordToolKitContext<*>)
fun CommandRegistrationScope.leaderboardCommand(leaderboardMenu: MessageMenu<Unit>) = localizedWith<LeaderboardCommandLocalization> {
    slashCommand("leaderboard", localized { description() }) {
        execute(ReplyMode.Defer(false)) {
            replyMenu(leaderboardMenu)
        }
    }
}

@LocalizationBundle("command", ".leaderboard")
internal interface LeaderboardCommandLocalization {
    fun description(): String
}
