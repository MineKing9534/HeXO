package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.command.ReplyMode
import de.mineking.discord.command.RootCommandRegistrationScope
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.userId
import de.mineking.hexo.game.model.profile.ProfileId
import net.dv8tion.jda.api.entities.User

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun RootCommandRegistrationScope.profileUserCommand(profileMenu: MessageMenu<ProfileId>) = localizedWith<ProfileUserCommandLocalization> {
    userCommand(localized { name() }) {
        execute(ReplyMode.Defer(false)) {
            val profileId = context.main.accountLinkRepository?.getHexoProfile(interaction.target.userId)
                ?: bind().profileNotLinked().throwValidationError(interaction.target)

            replyMenu(profileMenu, profileId)
        }
    }
}

@LocalizationBundle("context", ".profile")
internal interface ProfileUserCommandLocalization {
    fun name(): String

    fun profileNotLinked(): ErrorResponseMessage<User>
}
