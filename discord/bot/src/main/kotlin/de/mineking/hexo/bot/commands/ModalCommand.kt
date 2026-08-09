package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.SlashCommand
import de.mineking.discord.commands.localizedSlashCommand
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.components.modal.textInput
import de.mineking.discord.ui.builder.components.modal.withLocalizedLabel
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.modal.getValue
import de.mineking.discord.ui.modal.map
import de.mineking.discord.ui.modal.replyModal
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedModal
import de.mineking.discord.ui.render
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.bot.main
import de.mineking.hexo.bot.utils.fetchUserThemeData
import de.mineking.hexo.bot.utils.parseTheme
import de.mineking.hexo.bot.utils.replyRichHexoNotation
import de.mineking.hexo.bot.utils.themeSelect
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionContextType

private fun UIManager.renderHexoModal() = registerLocalizedModal<Interaction, ModalCommandLocalization>("hexo") { _ ->
    render {
        val interaction = parameter({ error("") }, { it }, { this })
        localize(interaction.userLocale)
    }

    val theme by +main.run {
        themeSelect(fetchUserThemeData()).withLocalizedLabel().map { value ->
            main.run {
                value.single().parseTheme(terminate = ::terminateRender).theme
            }
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
