package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.command.CommandRegistrationScope
import de.mineking.discord.command.ReplyMode
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.components.LabelLocalization
import de.mineking.discord.extensions.components.RequiredCheckboxLocalization
import de.mineking.discord.extensions.components.TextInputLocalization
import de.mineking.discord.extensions.components.label
import de.mineking.discord.extensions.components.requiredCheckbox
import de.mineking.discord.extensions.components.stringSelect
import de.mineking.discord.extensions.components.textInput
import de.mineking.discord.extensions.localization.localizeForUser
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.ActionScope
import de.mineking.discord.menu.Defer
import de.mineking.discord.menu.action
import de.mineking.discord.menu.components.onlyIf
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.components.MessageComponentScope
import de.mineking.discord.menu.message.components.actionRow
import de.mineking.discord.menu.message.components.button
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.message.components.mediaGallery
import de.mineking.discord.menu.message.components.menuButton
import de.mineking.discord.menu.message.components.modalButton
import de.mineking.discord.menu.message.components.section
import de.mineking.discord.menu.message.components.separator
import de.mineking.discord.menu.modal.components.optionalTextInput
import de.mineking.discord.menu.modal.modalValue
import de.mineking.discord.menu.renderOnly
import de.mineking.discord.menu.runtime.MenuRuntime
import de.mineking.discord.menu.state.MutableState
import de.mineking.discord.menu.state.ref
import de.mineking.discord.menu.state.state
import de.mineking.discord.menu.submodal
import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.mutable
import de.mineking.hexo.board.parse.notation.CombinedNotationParser
import de.mineking.hexo.board.render.image.theme.BaseTheme
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.board.rotate
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.IconEmojis
import de.mineking.hexo.discord.bot.config.CustomTheme
import de.mineking.hexo.discord.bot.config.CustomThemeSelector
import de.mineking.hexo.discord.bot.config.ThemeOverrideValue
import de.mineking.hexo.discord.bot.config.UserThemeSelection
import de.mineking.hexo.discord.bot.throwValidationError
import de.mineking.hexo.discord.bot.userId
import de.mineking.hexo.discord.bot.utils.ThemeId
import de.mineking.hexo.discord.bot.utils.backButton
import de.mineking.hexo.discord.bot.utils.boardImage
import de.mineking.hexo.discord.bot.utils.fetchUserThemeData
import de.mineking.hexo.discord.bot.utils.resolve
import de.mineking.hexo.discord.bot.utils.themeId
import de.mineking.hexo.discord.bot.utils.themeSelect
import de.mineking.hexo.utils.coroutines.completeImmediately
import de.mineking.hexo.utils.types.isSuccess
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.separator.Separator
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

@OptIn(InternalBoardApi::class)
private val DEMO_BOARD = completeImmediately {
    CombinedNotationParser.Default.parse(
        """
        5(!)o(!)/-x-(o)x(o)/oo(q7o)o(q3)xo(x)x(x)/x.oo(<2).x/-o(d2)x.x/o.oo(<4o)x[*]o/x.x.(q6x)/-(>6x) || p CCW x A0 B0 o C0 B3.0 x A1 C1.2 o B1.1 C5.1 x B1.0 C1.0 o A4 D1.0 x D3 C1.1 o E3 C2.0 x B1 C2 o D1.2 C5.2 x E4 F5
        """,
    ).rotate(1).mutable().focusWinningRows()
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private fun MenuRuntime.themeMenu() = registerMessageMenu("theme") {
    localizeForUser()
    val localization = translation<ThemeCommandLocalization>()

    val (customThemes, current) = fetchUserThemeData()
    var theme by state(initial = current.themeId)
    val resolvedTheme = theme.resolve() ?: current.also { theme = it.themeId }

    container {
        text {
            h1(":art: ${localization.title()}")
        }
        separator(Separator.Spacing.LARGE)

        renderOnly {
            mediaGallery(boardImage(DEMO_BOARD, resolvedTheme))
        }

        separator(Separator.Spacing.LARGE)
        actionRow {
            themeSelect(
                action<_, _>("theme") { theme = it.themeId },
                customThemes = customThemes,
                selected = theme,
                current = current.themeId,
            )
        }

        separator(invisible = true)
        actionRow {
            button(
                action("select") {
                    val result = context.main.userThemeRepository
                        ?.setCurrentUserTheme(interaction.user.userId, theme.toSelection())
                        ?: localization.selectError().throwValidationError()

                    if (!result.isSuccess()) {
                        localization.selectError().throwValidationError()
                    }

                    rerender()
                },
                style = ButtonStyle.SUCCESS,
                emoji = context.emojis[IconEmojis.Check],
                label = localization.selectLabel(),
                disabled = onlyIf(current.themeId == theme),
            )

            deleteButton(resolvedTheme)
            editButton(resolvedTheme)
            createButton(customThemes, theme.ref)
        }
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun CommandRegistrationScope.themeCommand() = localizedWith<ThemeCommandLocalization> {
    val menu = context.menuRuntime.themeMenu()

    slashCommand("theme", localized { commandDescription() }) {
        execute(ReplyMode.Defer(true)) {
            replyMenu(menu)
        }
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun MessageComponentScope<Button>.deleteButton(theme: Theme) {
    val localization = translation<ThemeCommandLocalization>()

    modalButton(
        "delete",
        style = ButtonStyle.DANGER,
        emoji = context.emojis[IconEmojis.Delete],
        label = localization.deleteLabel(),
        disabled = onlyIf(theme !is CustomTheme),
    ) {
        check(theme is CustomTheme)

        title(localization.deleteConfirmTitle())
        requiredCheckbox(localization = localization.deleteConfirmLabel(theme.name))

        onSubmit {
            val result = interaction.user.userId.run {
                context.main.userThemeRepository?.deleteThemeById(CustomThemeSelector.Id(theme.id))
            } ?: return@onSubmit

            if (!result.isSuccess()) {
                localization.deleteError().throwValidationError()
            }
        }
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private fun MessageComponentScope<Button>.createButton(customThemes: List<CustomTheme>, selected: MutableState<ThemeId>) {
    val localization = translation<ThemeCommandLocalization>()
    var selected by selected

    modalButton(
        "create",
        emoji = context.emojis[IconEmojis.Create],
        label = localization.createLabel(),
    ) {
        title(localization.createTitle())

        val base = modalValue<Theme> {
            label(localization.createBaseLabel()) {
                themeSelect(it.bind(), customThemes, selected = selected, current = null)
            }
        }

        val name = modalValue<String> {
            textInput(it.bind(), localization.createNameLabel())
        }

        onSubmit {
            val result = context.main.userThemeRepository?.createCustomTheme(interaction.user.userId, name.value(), base.value())
                ?: return@onSubmit

            if (!result.isSuccess()) {
                localization.createError().throwValidationError(name.value())
            }

            selected = ThemeId.Custom(result.value.id)
        }
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private fun MessageComponentScope<Button>.editButton(selected: Theme) {
    val localization = translation<ThemeCommandLocalization>()

    menuButton(
        "edit",
        style = ButtonStyle.PRIMARY,
        emoji = context.emojis[IconEmojis.Edit],
        label = localization.editLabel(),
        defer = Defer.Edit,
        disabled = selected !is CustomTheme,
    ) { back ->
        check(selected is CustomTheme)

        val modals = mapOf(
            "color" to themeParameterModal("color", selected, { editModalColorLabel() }) { ThemeOverrideValue.ColorValue(Color.parse(it)) },
            "double" to themeParameterModal("double", selected, { editModalDoubleLabel() }) { ThemeOverrideValue.DoubleValue(it.toDouble()) },
        )

        @Serializable
        data class PropertySelection(val name: String, val type: String)
        val edit = action<PropertySelection>("edit", defer = Defer.None) { (name, type) ->
            val modal = modals.getValue(type)
            replyModal(modal, name)
            rerender() // Clear selection
        }

        container {
            section(accessory = { backButton(back) }) {
                text {
                    h1(":art: ${localization.editTitle()}")
                }
            }

            separator(Separator.Spacing.LARGE)
            mediaGallery(boardImage(DEMO_BOARD, selected))
            separator(Separator.Spacing.LARGE)

            actionRow {
                stringSelect<_, _>(edit.map { it.single() }) {
                    selected.delegate::class.primaryConstructor!!.parameters.forEach { param ->
                        val name = param.name!!
                        val type = param.type.jvmErasure

                        option(
                            value = PropertySelection(name, type.simpleName!!.lowercase()),
                            label = name,
                            description = "${selected.delegate.propertyValue(name)}",
                            emoji = when (type) {
                                Color::class -> context.emojis[IconEmojis.Palette]
                                Double::class -> context.emojis[IconEmojis.Number]
                                else -> null
                            },
                        )
                    }
                }
            }
        }
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private fun MessageComponentScope<*>.themeParameterModal(
    name: String,
    selected: Theme,
    label: ThemeCommandLocalization.() -> LabelLocalization<Unit>,
    parse: (String) -> ThemeOverrideValue,
) = submodal<String>(name, defer = Defer.Edit) {
    val property by state(fromInput { input })

    val localization = translation<ThemeCommandLocalization>()
    title(localization.editModalTitle(property))

    check(selected is CustomTheme)
    val default = "${selected.base.theme.propertyValue(property)}"

    val value = modalValue<ThemeOverrideValue> {
        label(localization.label()) {
            optionalTextInput(
                it.bind().map { value ->
                    parse(value.orEmpty().ifBlank { default })
                },
                value = selected.overrides[property]?.value?.toString() ?: "",
                placeholder = default,
            )
        }
    }

    onSubmit {
        try {
            updateTheme(context.main, selected, property, value.value())
            replaceParent()
        } catch (e: IllegalArgumentException) {
            localization.editErrorInvalidValue().throwValidationError(InvalidValueParameter(property, e.message))
        }
    }
}

private fun BaseTheme.propertyValue(property: String): Any {
    @Suppress("UNCHECKED_CAST")
    val themeProperty = this::class.memberProperties
        .first { it.name == property } as KProperty1<BaseTheme, Any?>

    return requireNotNull(themeProperty.get(this))
}

context(scope: ActionScope<*>)
private suspend fun updateTheme(
    main: HeXODiscordBot,
    selected: CustomTheme,
    property: String,
    value: ThemeOverrideValue,
) {
    val result = scope.interaction.user.userId.run {
        main.userThemeRepository?.updateCustomThemeById(
            CustomThemeSelector.Id(selected.id),
            selected.copy(overrides = selected.overrides + (property to value)),
        )
    } ?: return

    check(result.isSuccess())
}

internal data class InvalidValueParameter(val property: String, val message: String?)

@LocalizationBundle("theme")
internal interface ThemeCommandLocalization {
    fun commandDescription(): String

    fun title(): String

    fun selectLabel(): String
    fun selectError(): ErrorResponseMessage<Unit>

    fun deleteLabel(): String
    fun deleteConfirmTitle(): String
    fun deleteConfirmLabel(parameter: String): RequiredCheckboxLocalization<String>
    fun deleteError(): ErrorResponseMessage<Unit>

    fun createLabel(): String
    fun createTitle(): String
    fun createNameLabel(): TextInputLocalization<Unit>
    fun createBaseLabel(): LabelLocalization<Unit>
    fun createError(): ErrorResponseMessage<String>

    fun editLabel(): String
    fun editTitle(): String
    fun editModalTitle(property: String): String
    fun editModalDoubleLabel(): LabelLocalization<Unit>
    fun editModalColorLabel(): LabelLocalization<Unit>
    fun editErrorInvalidValue(): ErrorResponseMessage<InvalidValueParameter>
}

private fun ThemeId.toSelection() = when (this) {
    is ThemeId.Custom -> UserThemeSelection.Custom(CustomThemeSelector.Id(id))
    is ThemeId.Default -> UserThemeSelection.Default(theme)
}
