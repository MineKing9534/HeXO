package de.mineking.hexo.bot.menus

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.append
import de.mineking.discord.ui.builder.components.buildTextDisplay
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.actionRow
import de.mineking.discord.ui.builder.components.message.button
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.link
import de.mineking.discord.ui.builder.components.message.section
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.message.thumbnail
import de.mineking.discord.ui.builder.h1
import de.mineking.discord.ui.getValue
import de.mineking.discord.ui.initialize
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.disableComponents
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.render
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.bot.escapeMarkdown
import de.mineking.hexo.bot.utils.MessageColor
import de.mineking.hexo.bot.utils.effectiveLocale
import de.mineking.hexo.bot.utils.respond
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileRepository
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.getDiscordProfile
import dev.freya02.jda.emojis.unicode.Emojis
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder

internal const val FALLBACK_IMAGE_URL = "https://cdn.discordapp.com/embed/avatars/0.png?size=128"

data class ProfileMenuParameter(val event: IReplyCallback, val id: ProfileId)

fun UIManager.profileMenu(
    profileRepository: ProfileRepository,
    accountLinkRepository: AccountLinkRepository?,
) = registerLocalizedMenu<ProfileMenuParameter, ProfileMenuLocalization>("profile") { localization ->
    var id by state(ProfileId(""))
    initialize {
        id = it.id
    }

    val locale = parameter({ DiscordLocale.UNKNOWN }, { it.event.effectiveLocale }, { event.effectiveLocale })
    localize(locale) // Predefine locale for potential error handling

    val reloadButton = register(
        button("reload", emoji = Emojis.RECYCLE) {
            disableComponents(message).queue()
            forceUpdate()
        },
    )

    message(MessageEditBuilder().setAllowedMentions(emptyList()))

    render {
        val (profile, linkedAccount) = coroutineScope {
            val profile = async { profileRepository.getProfile(id) }
            val linkedAccount = async { accountLinkRepository?.getDiscordProfile(id) }

            profile.await() to linkedAccount.await()
        }

        val event = parameter({ error("") }, { it.event }, { event })
        if (profile == null) {
            event.respond(MessageColor.Error, localization.errorProfileNotFound(event.effectiveLocale, id))
            terminateRender()
        }

        localize(locale) {
            bindParameter("profile", profile)
        }

        +container {
            +section(accessory = thumbnail(profile.image ?: FALLBACK_IMAGE_URL)) {
                +buildTextDisplay {
                    +h1 {
                        val rank = when (profile.statistics.rating.worldRank) {
                            1 -> Emojis.FIRST_PLACE.formatted
                            2 -> Emojis.SECOND_PLACE.formatted
                            3 -> Emojis.THIRD_PLACE.formatted
                            null -> "[:question:]"
                            else -> "[#${profile.statistics.rating.worldRank}]"
                        }

                        append("$rank ")
                        append(profile.displayName.escapeMarkdown())

                        if (linkedAccount != null) append(" (${linkedAccount.asMention})")

                        append(" ".repeat(5))
                        append(ZERO_WIDTH_SPACE)
                    }
                }
                +localizedTextDisplay("stats")
            }
            +separator()
            +actionRow(
                link("view", url = profile.url, emoji = Emojis.GLOBE_WITH_MERIDIANS),
                reloadButton,
            )
        }
    }
}

interface ProfileMenuLocalization : LocalizationFile {
    @Localize
    fun errorProfileNotFound(@Locale locale: DiscordLocale, @LocalizationParameter id: ProfileId): String
}
