package de.mineking.hexo.discord.bot.menus

import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.components.LabelLocalization
import de.mineking.discord.extensions.components.enumSelect
import de.mineking.discord.extensions.components.neutralSwitch
import de.mineking.discord.extensions.localization.localizeForUser
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.action
import de.mineking.discord.menu.components.StringSelectComponentScope
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.components.actionRow
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.message.components.fileDisplay
import de.mineking.discord.menu.message.components.section
import de.mineking.discord.menu.message.components.separator
import de.mineking.discord.menu.renderOnly
import de.mineking.discord.menu.runtime.MenuRuntime
import de.mineking.discord.menu.state.MutableState
import de.mineking.discord.menu.state.ref
import de.mineking.discord.menu.state.state
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.NotationEmojis
import de.mineking.hexo.discord.bot.utils.BoardReference
import de.mineking.hexo.discord.bot.utils.NotationType
import de.mineking.hexo.discord.bot.utils.decode
import net.dv8tion.jda.api.components.separator.Separator.Spacing
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.FileUpload

data class NotationMenuParameter(
    val reference: BoardReference,
    val notationType: NotationType,
)

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun MenuRuntime.notationMenu() = registerMessageMenu<NotationMenuParameter>("notation") {
    localizeForUser()
    val localization = translation<NotationMenuLocalization>()

    var reference by state<BoardReference>(fromInput { input.reference })
    var notationType by state<NotationType>(fromInput { input.notationType })
    var styled by state<Boolean>(true)

    container {
        section(accessory = {
            neutralSwitch(styled.ref, label = localization.styledLabel())
        }) {
            text {
                h1(":pencil: ${localization.title()}")
            }
        }

        renderOnly {
            val board = reference.decode(context.main.repositories.finishedGameRepository, notFound = {
                text(localization.gameNotFound())
                return@renderOnly
            })

            try {
                val styledNotation = notationType.renderer.render(board, styled)
                if (styledNotation.length > Message.MAX_CONTENT_LENGTH_COMPONENT_V2 - 100) {
                    val unstyledNotation = if (!styled) styledNotation else notationType.renderer.render(board, false)

                    if (unstyledNotation.length > Message.MAX_CONTENT_LENGTH_COMPONENT_V2 - 100) {
                        text(localization.notationTooLong())
                        fileDisplay(FileUpload.fromData(unstyledNotation.toByteArray(), "notation.txt"))
                    } else {
                        text(unstyledNotation)
                    }
                } else {
                    text(styledNotation)
                }
            } catch (e: HexoNotationException) {
                text(localization.notationError(e.message))
            }
        }

        separator(Spacing.LARGE)
        text {
            bold(localization.notationSelectLabel())
        }
        actionRow {
            notationSelect(notationType.ref)
        }
    }
}

context(_: DiscordToolKitContext<*>)
private suspend fun StringSelectComponentScope<*>.notationSelect(state: MutableState<NotationType>) {
    val localization = translation<NotationMenuLocalization>()
    var current by state

    enumSelect<_, NotationType>(action<_, _>("set-notation") {
        current = it.single()
    }) {
        val localization = localization.notationTypeLabel(it)
        default = it == current
        label = localization.label()
        description = localization.description()
        emoji = it.emoji
    }
}

context(context: DiscordToolKitContext<*>)
val NotationType.emoji get() = context.emojis[when (this) {
    NotationType.CompactRectilinear -> NotationEmojis.CRN
    NotationType.MultilineRectilinear -> NotationEmojis.MRN
    NotationType.Combined -> NotationEmojis.Combined
    NotationType.HTTTX -> NotationEmojis.HTTTX
    NotationType.Tyto -> NotationEmojis.Tyto
},]

@LocalizationBundle("notation")
internal interface NotationMenuLocalization {
    fun title(): String

    fun gameNotFound(): String
    fun notationTooLong(): String
    fun notationError(message: String): String

    fun notationSelectLabel(): String
    fun notationTypeLabel(parameter: NotationType): LabelLocalization<NotationType>
    fun styledLabel(): String
}
