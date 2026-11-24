package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.command.CommandRegistrationScope
import de.mineking.discord.command.ReplyMode
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.message.MessageMenu

context(_: DiscordToolKitContext<*>)
fun CommandRegistrationScope.accountLinkCommand(menu: MessageMenu<Unit>) = localizedWith<AccountLinkCommandLocalization> {
    slashCommand("link", localized { description() }) {
        execute(ReplyMode.Defer(true)) {
            replyMenu(menu)
        }
    }
}

@LocalizationBundle("command", ".accountLink")
internal interface AccountLinkCommandLocalization {
    fun description(): String
}
