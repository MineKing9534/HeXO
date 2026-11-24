package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.command.CommandRegistrationScope
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.components.LabelLocalization
import de.mineking.discord.extensions.components.label
import de.mineking.discord.extensions.localization.localizeForUser
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.extensions.reply
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.markdown.markdown
import de.mineking.discord.menu.Defer
import de.mineking.discord.menu.action
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.discord.menu.message.components.actionRow
import de.mineking.discord.menu.message.components.button
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.modal.ModalMenu
import de.mineking.discord.menu.modal.components.textInput
import de.mineking.discord.menu.modal.modalValue
import de.mineking.discord.menu.renderOnly
import de.mineking.discord.menu.renderOrElse
import de.mineking.discord.menu.runtime.MenuRuntime
import de.mineking.discord.menu.state.state
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.IconEmojis
import de.mineking.hexo.discord.bot.utils.MissingImages
import de.mineking.hexo.discord.bot.utils.compositeHexoNotation
import de.mineking.hexo.discord.bot.utils.fetchUserThemeData
import de.mineking.hexo.discord.bot.utils.resolve
import de.mineking.hexo.discord.bot.utils.themeId
import de.mineking.hexo.discord.bot.utils.themeSelect
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent

context(_: DiscordToolKitContext<HeXODiscordBot>)
private fun MenuRuntime.renderHexoModal(
    preview: () -> MessageMenu<String>,
) = registerModalMenu<String?>("hexo-message-edit", defer = Defer.None) {
    localizeForUser()
    val localization = translation<RenderHexoModalCommandLocalization>()
    val isInitial by state(fromInput { input == null })

    title(localization.modalTitle())

    val initial = fromInput { input }
    val content = modalValue {
        label(localization.notationLabel()) {
            renderOnly {
                textInput(
                    it.bind(),
                    value = initial(),
                    style = TextInputStyle.PARAGRAPH,
                    placeholder = """
                        Sword Opening:
                        ```
                        . . . . o
                         o . x x x o
                          . . . o
                        ```
                    """.trimIndent(),
                )
            }
        }
    }

    onSubmit {
        if (isInitial) {
            replyMenu(preview(), content.value(), ephemeral = true)
        } else {
            replaceMenu(preview(), content.value())
        }
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private fun MenuRuntime.hexoPreviewMenu(edit: () -> ModalMenu<String?>) = registerMessageMenu<String>("hexo-preview") {
    localizeForUser()
    val localization = translation<RenderHexoModalCommandLocalization>()

    var content: String? = null
    initialize {
        content = input
    }

    if (content == null) {
        val event = interaction as GenericComponentInteractionCreateEvent
        val components = event.message.components[0].asContainer().components
        content = markdown {
            components.forEach { component ->
                when (component) {
                    is TextDisplay -> raw(component.content)
                    is MediaGallery -> component.items.forEach {
                        val notation = it.description ?: return@forEach
                        if (it.isSpoiler) {
                            spoiler {
                                codeBlock(notation, language = "hexo")
                            }
                        } else {
                            codeBlock(notation, language = "hexo")
                        }
                    }
                }
            }
        }
    }

    // Define this action early since it doesn't need resolved themes
    val edit = action("edit", defer = Defer.None) {
        replyModal(edit(), content)
    }

    val (customThemes, current) = fetchUserThemeData()

    var theme by state(initial = current.themeId)
    val resolvedTheme = theme.resolve() ?: current.also { theme = it.themeId }

    val result = renderOrElse(Result.Error(MissingImages)) {
        container {
            compositeHexoNotation(content, resolvedTheme)
        }
    }

    text {
        bold(localization.selectThemeLabel())
    }

    actionRow {
        themeSelect(action<_, _>("theme") { theme = it.themeId }, customThemes, selected = theme, current = null)
    }

    actionRow {
        button(action<_, _>("send", defer = Defer.Edit) {
            interaction.hook.deleteOriginal().queue()
            reply {
                compositeHexoNotation(content, resolvedTheme)
            }
        }, style = ButtonStyle.PRIMARY, label = localization.send(), emoji = context.emojis[IconEmojis.Send], disabled = !result.isSuccess())

        button(edit, label = localization.edit(), emoji = context.emojis[IconEmojis.Edit])
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun CommandRegistrationScope.renderHexoModalCommand() = localizedWith<RenderHexoModalCommandLocalization> {
    slashCommand("message", localized { description() }) {
        lateinit var modal: ModalMenu<String?>
        lateinit var preview: MessageMenu<String>

        modal = context.menuRuntime.renderHexoModal { preview }
        preview = context.menuRuntime.hexoPreviewMenu { modal }

        execute {
            replyModal(modal, null)
        }
    }
}

@LocalizationBundle("modal", ".render")
internal interface RenderHexoModalCommandLocalization {
    fun description(): String

    fun modalTitle(): String
    fun notationLabel(): LabelLocalization<Unit>

    fun selectThemeLabel(): String
    fun send(): String
    fun edit(): String
}
