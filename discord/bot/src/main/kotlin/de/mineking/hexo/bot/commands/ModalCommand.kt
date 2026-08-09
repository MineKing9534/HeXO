package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.SlashCommand
import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.components.modal.textInput
import de.mineking.discord.ui.builder.components.modal.withLocalizedLabel
import de.mineking.discord.ui.builder.components.selectOption
import de.mineking.discord.ui.builder.components.stringSelect
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.modal.getValue
import de.mineking.discord.ui.modal.map
import de.mineking.discord.ui.modal.replyModal
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedModal
import de.mineking.discord.ui.render
import de.mineking.discord.ui.renderValue
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.bot.CustomEmoji
import de.mineking.hexo.bot.main
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.parseTheme
import de.mineking.hexo.bot.utils.replyRichHexoNotation
import dev.freya02.jda.emojis.unicode.Emojis
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionContextType

private fun UIManager.renderHexoModal() = registerLocalizedModal<Interaction, ModalCommandLocalization>("hexo") { _ ->
    render {
        val interaction = parameter({ error("") }, { it }, { this })
        localize(interaction.userLocale)
    }

    val options = renderValue {
        val repository = main.userThemeRepository ?: return@renderValue null

        val user = parameter({ error("") }, { it.user.userId }, { user.userId })
        val (customThemes, default) = coroutineScope {
            val themes = async { repository.listUserThemes(user) }
            val default = async { main.getUserTheme(user) }

            themes.await() to default.await()
        }

        val customOptions = customThemes.map {
            selectOption(
                value = it.id.value,
                label = it.name,
                default = it == default,
                description = null,
                emoji = Emojis.ART,
            )
        }

        val defaultOptions = DefaultTheme.entries.map {
            selectOption(
                value = it.name,
                default = it.theme == default,
                description = null,
                emoji = when (it) {
                    DefaultTheme.HDS -> main.emojiManager[CustomEmoji.ThemeHDS]
                    DefaultTheme.HTTTX -> main.emojiManager[CustomEmoji.ThemeHTTTX]
                    DefaultTheme.Tyto -> main.emojiManager[CustomEmoji.ThemeTyto]
                },
            )
        }

        customOptions + defaultOptions
    }

    val theme by +stringSelect(
        name = "theme",
        placeholder = null,
        options = options.orEmpty(),
    ).withLocalizedLabel().map { value ->
        main.run {
            value.single().parseTheme(terminate = ::terminateRender)
        }
    }

    val content by +textInput("input", style = TextInputStyle.PARAGRAPH).withLocalizedLabel()

    execute {
        main.run {
            replyRichHexoNotation(content, theme)
        }
    }
}

fun modalCommand(): SlashCommand = { parent ->
    val modal = manager.get<UIManager>().renderHexoModal()
    localizedSlashCommand<ModalCommandLocalization>("render") {
        integrationTypes(IntegrationType.ALL)
        interactionContextTypes(InteractionContextType.ALL)

        execute {
            replyModal(modal, event).queue()
        }
    }(parent)
}

interface ModalCommandLocalization : LocalizationFile
