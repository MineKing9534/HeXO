package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.OptionConfig
import de.mineking.discord.commands.choice
import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.commands.map
import de.mineking.discord.commands.nullableStringOption
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.replyMenu
import de.mineking.hexo.bot.menus.ProfileMenuParameter
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.finalErrorResponse
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.profile.ProfileRepository
import de.mineking.hexo.hds.model.profile.getProfileByName
import de.mineking.hexo.link.AccountLinkRepository
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

fun profileCommand(
    accountLinkRepository: AccountLinkRepository?,
    profileRepository: ProfileRepository,
    profileMenu: MessageMenu<ProfileMenuParameter, *>,
) = localizedSlashCommand<ProfileCommandLocalization>("profile") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val id = profileIdOption(accountLinkRepository, profileRepository, "name")

    execute {
        val id = id() ?: finalErrorResponse(localization.responseErrorNotFoundGeneric(userLocale))

        deferReply().queue()
        replyMenu(profileMenu, ProfileMenuParameter(event, id)).queue()
    }
}

private val profileIdPattern = """(?:.*/)?([a-f0-9]{24})""".toRegex()

private fun OptionConfig.profileIdOption(
    accountLinkRepository: AccountLinkRepository?,
    profileRepository: ProfileRepository,
    name: String,
) = nullableStringOption(name) {
    val search = profileRepository.getProfilesByName(currentValue ?: "")
    val options = search.map { choice(it.id.value, it.displayName) }
    replyChoices(options)
}.map { input ->
    if (input == null) {
        return@map accountLinkRepository?.getHexoProfile(user.userId)
    }

    val profileIdMatch = profileIdPattern.matchEntire(input)
    when {
        profileIdMatch != null -> ProfileId(profileIdMatch.groupValues[1])
        else -> profileRepository.getProfileByName(input)?.id
    }
}

interface ProfileCommandLocalization : LocalizationFile {
    @Localize
    fun responseErrorNotFoundGeneric(@Locale locale: DiscordLocale): String

    @Localize
    fun responseErrorNotFoundUser(@Locale locale: DiscordLocale, @LocalizationParameter user: User): String
}
