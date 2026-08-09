package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.localizedMenuCommand
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.InitialMenuContext
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.ButtonColor
import de.mineking.discord.ui.builder.components.message.actionRow
import de.mineking.discord.ui.builder.components.message.button
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.mediaGallery
import de.mineking.discord.ui.builder.components.message.menuButton
import de.mineking.discord.ui.builder.components.message.modalButton
import de.mineking.discord.ui.builder.components.message.section
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.modal.requiredCheckbox
import de.mineking.discord.ui.builder.components.modal.textInput
import de.mineking.discord.ui.builder.components.modal.withLocalizedLabel
import de.mineking.discord.ui.enabledIf
import de.mineking.discord.ui.getValue
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.MessageMenuConfig
import de.mineking.discord.ui.modal.createModalComponent
import de.mineking.discord.ui.modal.getValue
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.board.parse.parseRectilinearNotation
import de.mineking.hexo.board.render.image.theme.DefaultTheme
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.EMOJI_THEME_SELECTED
import de.mineking.hexo.bot.utils.MessageColor
import de.mineking.hexo.bot.utils.asMediaGalleryItem
import de.mineking.hexo.bot.utils.bindLocalizationParameter
import de.mineking.hexo.bot.utils.effectiveLocale
import de.mineking.hexo.bot.utils.fetchUserThemeData
import de.mineking.hexo.bot.utils.parseTheme
import de.mineking.hexo.bot.utils.respond
import de.mineking.hexo.bot.utils.themeSelect
import de.mineking.hexo.discord.bot.config.CustomTheme
import de.mineking.hexo.discord.bot.config.CustomThemeId
import de.mineking.hexo.discord.bot.config.CustomThemeSelector
import de.mineking.hexo.discord.bot.config.UserThemeSelection
import de.mineking.hexo.discord.bot.config.base
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import dev.freya02.jda.emojis.unicode.Emojis
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionContextType

private val DEMO_BOARD = "4(p4o)-(q4x)/3x(!)o(!)/3(!).(x).(o)/3xo/.(>5)".parseRectilinearNotation()

context(main: HeXODiscordBot)
fun themeCommand() = localizedMenuCommand<ThemeCommandLocalization>("theme") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val locale = parameter({ DiscordLocale.UNKNOWN }, { it.effectiveLocale }, { effectiveLocale })
    localize(locale)

    val (themes, current) = fetchUserThemeData()
    var selected by state(current.id)

    +container {
        +localizedTextDisplay("header")
        +separator(spacing = Separator.Spacing.LARGE)

        val selectedTheme = when (context) {
            is InitialMenuContext -> current.also { selected = it.id }
            else -> when (val temp = selected) {
                is ThemeId.Default -> temp.theme.theme
                is ThemeId.Custom -> main.userThemeRepository!!
                    .getThemeById(CustomThemeSelector.Id(temp.id))
                    .let { it as? Result.Success }
                    ?.value
                    ?: Theme.Default.also { selected = it.id }
            }
        }

        +mediaGallery(DEMO_BOARD.asMediaGalleryItem(selectedTheme))

        +separator(spacing = Separator.Spacing.LARGE)
        +actionRow(themeSelect(
            "theme",
            customThemes = themes,
            isSelected = { it.id == selected },
            isCurrent = { it == current },
        ) {
            selected = event.values.single().toThemeId()
        })

        +separator(invisible = true)

        +actionRow {
            +button("select", color = ButtonColor.GREEN, emoji = EMOJI_THEME_SELECTED) {
                deferEdit().queue()

                val result = main.userThemeRepository?.setCurrentUserTheme(user.userId, selected.toSelection()) ?: return@button

                if (!result.isSuccess()) {
                    respond(MessageColor.Error, localization.responseErrorThemeChange(userLocale))
                    terminateRender()
                }

                forceUpdate()
            }.enabledIf(selectedTheme != current)

            +deleteButton(selectedTheme)
            +editButton(selectedTheme)
            +createButton(themes, selectedTheme)
        }
    }
}

context(main: HeXODiscordBot)
private fun MessageMenuConfig<*, *>.deleteButton(selected: Theme) = modalButton(
    "delete",
    color = ButtonColor.RED,
    emoji = Emojis.WASTEBASKET,
    component = createModalComponent {
        require(selected is CustomTheme)
        bindLocalizationParameter("name") { selected.name }
        +requiredCheckbox("confirm", description = null).withLocalizedLabel()

        produce {}
    },
) {
    val result = user.userId.run {
        require(selected is CustomTheme)
        main.userThemeRepository?.deleteThemeById(CustomThemeSelector.Id(selected.id))
    } ?: return@modalButton

    if (!result.isSuccess()) {
        // TODO
        terminateRender()
    }
}.enabledIf(selected is CustomTheme)

context(main: HeXODiscordBot)
private fun MessageMenuConfig<out Interaction, *>.createButton(
    customThemes: List<CustomTheme>,
    selected: Theme,
) = modalButton("create", emoji = Emojis.MEMO, component = createModalComponent {
    val name by +textInput("name").withLocalizedLabel()
    val base by +main.run {
        this@createButton.themeSelect(
            "base",
            customThemes = customThemes,
            isSelected = { it == selected },
            isCurrent = { false },
        ).withLocalizedLabel()
    }

    produce { name to base }
}) { (name, base) ->
    val result = main.userThemeRepository?.createCustomTheme(user.userId, name, base.single().parseTheme(terminate = ::terminateRender).theme)
        ?: return@modalButton

    if (!result.isSuccess()) {
        // TODO
    }
}

context(main: HeXODiscordBot)
private fun MessageMenuConfig<*, *>.editButton(selected: Theme) = menuButton(
    "edit",
    emoji = Emojis.SCREWDRIVER,
    color = ButtonColor.BLUE,
) { back ->
    +container {
        +section(
            accessory = back.asButton("back", emoji = Emojis.LEFT_ARROW),
            localizedTextDisplay("header"),
        )
        +separator(spacing = Separator.Spacing.LARGE)

        +mediaGallery(DEMO_BOARD.asMediaGalleryItem(selected))

        +separator(spacing = Separator.Spacing.LARGE)
    }
}.enabledIf(selected is CustomTheme)

interface ThemeCommandLocalization : LocalizationFile {
    @Localize
    fun responseErrorThemeChange(@Locale locale: DiscordLocale): String
}

@Serializable
private sealed interface ThemeId {
    @Serializable
    @SerialName("custom")
    data class Custom(val id: CustomThemeId) : ThemeId

    @Serializable
    @SerialName("default")
    data class Default(val theme: DefaultTheme) : ThemeId
}

private val Theme.id get() = when (this) {
    is CustomTheme -> ThemeId.Custom(id)
    else -> ThemeId.Default(base)
}

private fun ThemeId.toSelection() = when (this) {
    is ThemeId.Custom -> UserThemeSelection.Custom(CustomThemeSelector.Id(id))
    is ThemeId.Default -> UserThemeSelection.Default(theme)
}

private fun String.toThemeId() = DefaultTheme.entries
    .find { it.name == this }
    ?.let { ThemeId.Default(it) }
    ?: ThemeId.Custom(CustomThemeId(this))
