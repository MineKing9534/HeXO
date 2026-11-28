package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.LocalizedString
import de.mineking.discord.command.CommandRegistrationScope
import de.mineking.discord.command.ReplyMode
import de.mineking.discord.command.SlashCommandBuilder
import de.mineking.discord.command.option.CommandChoice
import de.mineking.discord.command.option.OptionRequirement
import de.mineking.discord.command.option.optional
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.throwValidationError
import de.mineking.hexo.discord.bot.userId
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.getProfileByName
import de.mineking.hexo.utils.types.map
import de.mineking.hexo.utils.types.orElse

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun CommandRegistrationScope.profileCommand(profileMenu: MessageMenu<ProfileId>) = localizedWith<ProfileCommandLocalization> {
    slashCommand("profile", localized { description() }) {
        val profile = profileIdOption("name", localized { optionDescription() }, optional {
            context.main.accountLinkRepository?.getHexoProfile(interaction.user.userId)
                ?: bind().profileNotLinked().throwValidationError()
        })

        execute(ReplyMode.Defer(false)) {
            replyMenu(profileMenu, profile.value())
        }
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private fun <T> SlashCommandBuilder.profileIdOption(
    name: String,
    description: LocalizedString,
    requirement: OptionRequirement<ProfileId, T>,
) = stringOption(name, description, requirement.mapInput { input ->
    context.main.repositories.profileRepository.getProfileByName(input)
        .map { it.id }
        .orElse { ProfileId(input) }
}) {
    autocomplete {
        val search = context.main.repositories.profileRepository.getProfilesByName(focusedText)
        search.map {
            CommandChoice(value = it.id.value, label = it.displayName)
        }
    }
}

@LocalizationBundle("slash", ".profile")
internal interface ProfileCommandLocalization {
    fun description(): String
    fun optionDescription(): String

    fun profileNotLinked(): ErrorResponseMessage<Unit>
}
