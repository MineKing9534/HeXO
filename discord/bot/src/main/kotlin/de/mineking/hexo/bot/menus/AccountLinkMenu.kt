package de.mineking.hexo.bot.menus

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.ButtonColor
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.modalButton
import de.mineking.discord.ui.builder.components.message.section
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.modal.requiredCheckbox
import de.mineking.discord.ui.builder.components.modal.textInput
import de.mineking.discord.ui.builder.components.modal.withLocalizedLabel
import de.mineking.discord.ui.localizeForUser
import de.mineking.discord.ui.message.MessageMenuConfig
import de.mineking.discord.ui.modal.createModalComponent
import de.mineking.discord.ui.modal.getValue
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.renderValue
import de.mineking.discord.ui.terminateRender
import de.mineking.discord.utils.await
import de.mineking.hexo.bot.CustomEmoji
import de.mineking.hexo.bot.main
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.MessageColor
import de.mineking.hexo.bot.utils.bindLocalizationParameter
import de.mineking.hexo.bot.utils.respond
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.profile.ProfileRepository
import de.mineking.hexo.hds.model.profile.getProfileByName
import de.mineking.hexo.link.AccountLinkRepository
import de.mineking.hexo.link.oauth2.DiscordUserAuthenticationRepository
import de.mineking.hexo.utils.types.isSuccess
import dev.freya02.jda.emojis.unicode.Emojis
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback

private val IMAGE_URL_PATTERN = """https://cdn\.discordapp\.com/avatars/(\d+)/.*\.(?:png|jpg|webp|gif)(?:\?size=\d+)?""".trimMargin().toRegex()

fun UIManager.accountLinkMenu(
    discordAuthRepository: DiscordUserAuthenticationRepository,
    accountLinkRepository: AccountLinkRepository,
    profileRepository: ProfileRepository,
) = registerLocalizedMenu<IModalCallback, AccountLinkMenuLocalization>("link") { localization ->
    localizeForUser()

    // Creating separate components for each and conditionally rendering them avoids having to query the state again in
    // component handlers to figure out which menu to show
    val linkModalButton = register(linkModalButton(profileRepository, accountLinkRepository, localization))
    val unlinkConfirmModalButton = register(unlinkConfirmModalButton(accountLinkRepository))

    val authModalButton = register(authModalButton())
    val authRemoveConfirmModalButton = register(authRemoveConfirmModalButton(localization, discordAuthRepository))

    // Define this *after* the submenus, so that this is not executed for submenu renders
    val event = parameter({ null }, { it }, { event })
    val (profile, isAuthenticated) = renderValue {
        coroutineScope {
            val profile = async {
                val profileId = accountLinkRepository.getHexoProfile(event!!.user.userId)
                profileId?.let { profileRepository.getProfile(profileId) }
            }

            val isAuthenticated = async { discordAuthRepository.getAuthenticationStatus(event!!.user.userId) }

            profile.await() to isAuthenticated.await()
        }
    } ?: (null to false)

    localizeForUser {
        bindParameter("profile", profile)
        bindParameter("isAuthenticated", isAuthenticated)
    }

    +container {
        +localizedTextDisplay("title")
        +separator()
        +section(
            accessory = if (profile == null) linkModalButton else unlinkConfirmModalButton,
            localizedTextDisplay("link_status"),
        )
        +separator(invisible = true)
        +section(
            accessory = if (!isAuthenticated) authModalButton else authRemoveConfirmModalButton,
            localizedTextDisplay("auth_status"),
        )

        if (profile != null && !isAuthenticated) {
            +separator(spacing = Separator.Spacing.LARGE)
            +localizedTextDisplay("auth_explanation")
        }
    }
}

private fun MessageMenuConfig<out Interaction, *>.linkModalButton(
    profileRepository: ProfileRepository,
    linkRepository: AccountLinkRepository,
    localization: AccountLinkMenuLocalization,
) = modalButton(
    "link",
    color = ButtonColor.GREEN,
    emoji = menu.manager.main.emojiManager[CustomEmoji.Link],
    component = createModalComponent {
        +localizedTextDisplay("explanation")

        val event = this@linkModalButton.parameter({ null }, { it }, { event })
        val candidate = event?.user?.name?.let { profileRepository.getProfileByName(it) }
        val profileUrl by +textInput("url", value = candidate?.url).withLocalizedLabel()

        produce { profileUrl }
    },
) { profileUrl ->
    deferEdit().queue()

    val profileId = ProfileId(profileUrl.split("/").last())
    val profile = profileRepository.getProfile(profileId)
    if (profile == null) {
        respond(MessageColor.Error, localization.responseErrorProfileNotFound(userLocale, profileId), forceNew = true)
        terminateRender()
    }

    val match = profile.image?.let { IMAGE_URL_PATTERN.matchEntire(it) }
    if (match?.groupValues[1] != user.id) {
        respond(MessageColor.Error, localization.responseErrorProfileVerifyFailed(userLocale), forceNew = true)
        terminateRender()
    }

    if (!linkRepository.createLink(user.userId, profile.id).isSuccess()) {
        respond(MessageColor.Error, localization.responseErrorProfileLinkFailed(userLocale), forceNew = true)
        terminateRender()
    }
}

private fun MessageMenuConfig<*, *>.unlinkConfirmModalButton(linkRepository: AccountLinkRepository) = modalButton(
    "unlink",
    color = ButtonColor.RED,
    emoji = menu.manager.main.emojiManager[CustomEmoji.Unlink],
    component = requiredCheckbox("confirm", description = null).withLocalizedLabel(),
) {
    linkRepository.removeLinkedProfile(user.userId)
}

private fun MessageMenuConfig<out Interaction, *>.authModalButton() = modalButton(
    "auth",
    color = ButtonColor.GREEN,
    emoji = Emojis.LOCK,
    component = createModalComponent {
        bindLocalizationParameter("url") { this@authModalButton.menu.manager.main.publicUrl }
        +localizedTextDisplay("description")

        produce {}
    },
) {
}

private fun MessageMenuConfig<*, *>.authRemoveConfirmModalButton(
    localization: AccountLinkMenuLocalization,
    discordAuthRepository: DiscordUserAuthenticationRepository,
) = modalButton(
    "remove_auth",
    color = ButtonColor.RED,
    emoji = Emojis.UNLOCK,
    component = createModalComponent {
        +localizedTextDisplay("note")
        +requiredCheckbox("confirm", description = null).withLocalizedLabel()

        produce {}
    },
) {
    preventUpdate()
    deferEdit().queue()
    hook.deleteOriginal().await()
    respond(MessageColor.Success, localization.responseSuccessRevoke(userLocale), forceNew = true)

    discordAuthRepository.unauthenticateUser(user.userId)
}

interface AccountLinkMenuLocalization : LocalizationFile {
    @Localize
    fun responseErrorProfileNotFound(@Locale locale: DiscordLocale, @LocalizationParameter id: ProfileId): String

    @Localize
    fun responseErrorProfileVerifyFailed(@Locale locale: DiscordLocale): String

    @Localize
    fun responseErrorProfileLinkFailed(@Locale locale: DiscordLocale): String

    @Localize
    fun responseSuccessRevoke(@Locale locale: DiscordLocale): String
}
