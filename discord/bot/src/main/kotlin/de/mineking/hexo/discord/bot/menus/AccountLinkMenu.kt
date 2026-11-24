package de.mineking.hexo.discord.bot.menus

import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.components.LabelLocalization
import de.mineking.discord.extensions.components.RequiredCheckboxLocalization
import de.mineking.discord.extensions.components.label
import de.mineking.discord.extensions.components.requiredCheckbox
import de.mineking.discord.extensions.localization.localizeForUser
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.markdown.markdown
import de.mineking.discord.menu.Action
import de.mineking.discord.menu.Defer
import de.mineking.discord.menu.action
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.components.MessageComponentScope
import de.mineking.discord.menu.message.components.button
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.message.components.section
import de.mineking.discord.menu.message.components.separator
import de.mineking.discord.menu.modal.ModalRenderScope
import de.mineking.discord.menu.modal.components.textInput
import de.mineking.discord.menu.modal.modalValue
import de.mineking.discord.menu.renderOnly
import de.mineking.discord.menu.renderOrElse
import de.mineking.discord.menu.runtime.MenuRuntime
import de.mineking.discord.menu.submodal
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.IconEmojis
import de.mineking.hexo.discord.bot.throwValidationError
import de.mineking.hexo.discord.bot.userId
import de.mineking.hexo.game.model.profile.Profile
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileNotFoundError
import de.mineking.hexo.game.model.profile.getProfileByName
import de.mineking.hexo.utils.types.flatMap
import de.mineking.hexo.utils.types.isSuccess
import de.mineking.hexo.utils.types.orElse
import de.mineking.hexo.utils.types.orNull
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent

private val IMAGE_URL_PATTERN = """https://cdn\.discordapp\.com/avatars/(\d+)/.*\.(?:png|jpg|webp|gif)(?:\?size=\d+)?""".toRegex()

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun MenuRuntime.accountLinkMenu() = registerMessageMenu("link") {
    localizeForUser()
    val localization = translation<AccountLinkMenuLocalization>()

    val (profile, isAuthenticated) = renderOrElse(null to false) {
        val user = requireInteraction().user.userId
        coroutineScope {
            val profile = async {
                context.main.accountLinkRepository?.getHexoProfile(user)
                    .successIfNotNullOrElse(ProfileNotFoundError)
                    .flatMap { context.main.repositories.profileRepository.getProfile(it) }
                    .orNull()
            }

            val isAuthenticated = async { context.main.discordUserAuthenticationRepository?.hasTokens(user) ?: false }

            profile.await() to isAuthenticated.await()
        }
    }

    container {
        text {
            h1(":link: ${localization.title()}")
        }
        separator(Separator.Spacing.LARGE)

        linkSection(profile)
        separator(invisible = true)
        discordAuthSection(isAuthenticated)

        if (profile != null && !isAuthenticated) {
            separator(spacing = Separator.Spacing.LARGE)
            text(localization.linkAuthInfo())
        }
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private suspend fun MessageComponentScope<Section>.linkSection(profile: Profile?) {
    val localization = translation<AccountLinkMenuLocalization>()

    val link = submodalAction("link") {
        title(localization.linkTitle())
        text(localization.linkExplanation())

        val id = modalValue {
            label(localization.linkProfileIdLabel()) {
                renderOnly {
                    val candidate = context.main.repositories.profileRepository
                        .getProfileByName(requireInteraction().user.name)
                        .orNull()

                    textInput(it.bind(), placeholder = "https://hexo.did.science/profile/69bdd10f3a92bf78e1b6d7a0", value = candidate?.url)
                }
            }
        }

        onSubmit {
            val profileId = ProfileId(id.value().split("/").last())
            val profile = context.main.repositories.profileRepository.getProfile(profileId).orElse {
                localization.profileNotFound().throwValidationError(profileId.value)
            }

            val match = profile.image?.let { IMAGE_URL_PATTERN.matchEntire(it) }
            if (match?.groupValues[1] != interaction.user.id) {
                localization.profileVerificationFailed().throwValidationError()
            }

            if (context.main.accountLinkRepository?.createLink(interaction.user.userId, profile.id)?.isSuccess() != true) {
                localization.profileLinkFailed().throwValidationError()
            }
        }
    }

    val unlink = submodalAction("unlink") {
        title(localization.unlinkConfirmTitle())

        requiredCheckbox(localization = localization.unlinkConfirmLabel())

        onSubmit {
            context.main.accountLinkRepository?.removeLinkedProfile(interaction.user.userId)
        }
    }

    section(accessory = {
        if (profile != null) {
            button(
                unlink,
                style = ButtonStyle.DANGER,
                label = localization.unlink(),
                emoji = context.emojis[IconEmojis.Unlink],
            )
        } else {
            button(
                link,
                style = ButtonStyle.SUCCESS,
                label = localization.link(),
                emoji = context.emojis[IconEmojis.Link],
            )
        }
    }) {
        text(localization.linkStatus(profile?.let {
            markdown {
                link(it.displayName, url = it.url)
            }
        }))
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private suspend fun MessageComponentScope<Section>.discordAuthSection(isAuthenticated: Boolean) {
    val localization = translation<AccountLinkMenuLocalization>()

    val auth = submodalAction("auth") {
        title(localization.authorizationTitle())
        text(localization.authorizationExplanation("${context.main.publicUrl}/linked-roles"))
    }

    val revoke = submodalAction("revoke") {
        title(localization.revokeConfirmTitle())

        text(localization.revokeConfirmNote())
        requiredCheckbox(localization = localization.revokeConfirmLabel())

        onSubmit {
            context.main.discordUserAuthenticationRepository?.revoke(interaction.user.userId)
        }
    }

    section(accessory = {
        if (isAuthenticated) {
            button(
                revoke,
                style = ButtonStyle.DANGER,
                label = localization.revoke(),
                emoji = context.emojis[IconEmojis.Unlock],
            )
        } else {
            button(
                auth,
                style = ButtonStyle.SUCCESS,
                label = localization.authorize(),
                emoji = context.emojis[IconEmojis.Lock],
            )
        }
    }) {
        text(localization.authorizationStatus(isAuthenticated))
    }
}

private fun MessageComponentScope<*>.submodalAction(
    name: String,
    body: suspend ModalRenderScope<Unit>.() -> Unit,
): Action<GenericInteractionCreateEvent, Unit> {
    val modal = submodal(name, defer = Defer.Edit) {
        body()
        onSubmit {
            if (!hasReply) replaceParent()
        }
    }

    return action(name, defer = Defer.None) {
        replyModal(modal)
    }
}

@LocalizationBundle("accountLink")
internal interface AccountLinkMenuLocalization {
    fun title(): String

    fun link(): String
    fun linkTitle(): String
    fun linkExplanation(): String
    fun linkProfileIdLabel(): LabelLocalization<Unit>
    fun linkStatus(profileUrl: String?): String

    fun unlink(): String
    fun unlinkConfirmTitle(): String
    fun unlinkConfirmLabel(): RequiredCheckboxLocalization<Unit>

    fun authorize(): String
    fun authorizationTitle(): String
    fun authorizationExplanation(url: String): String
    fun authorizationStatus(status: Boolean): String

    fun revoke(): String
    fun revokeConfirmTitle(): String
    fun revokeConfirmNote(): String
    fun revokeConfirmLabel(): RequiredCheckboxLocalization<Unit>

    fun profileNotFound(): ErrorResponseMessage<String>
    fun profileVerificationFailed(): ErrorResponseMessage<Unit>
    fun profileLinkFailed(): ErrorResponseMessage<Unit>

    fun linkAuthInfo(): String
}
