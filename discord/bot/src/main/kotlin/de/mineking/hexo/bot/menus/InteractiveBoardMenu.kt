package de.mineking.hexo.bot.menus

import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.localize
import de.mineking.discord.localization.read
import de.mineking.discord.ui.MutableState
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.components.message.actionRow
import de.mineking.discord.ui.builder.components.message.button
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.mediaGallery
import de.mineking.discord.ui.builder.components.message.modalButton
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.message.toggleButton
import de.mineking.discord.ui.builder.components.modal.intInput
import de.mineking.discord.ui.builder.components.modal.localizedLabel
import de.mineking.discord.ui.builder.components.modal.unbox
import de.mineking.discord.ui.currentLocalizationConfig
import de.mineking.discord.ui.disabledIf
import de.mineking.discord.ui.getValue
import de.mineking.discord.ui.initialize
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.MessageComponent
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.MessageMenuConfig
import de.mineking.discord.ui.message.createMessageComponent
import de.mineking.discord.ui.message.parameter
import de.mineking.discord.ui.message.replyMenu
import de.mineking.discord.ui.message.withParameter
import de.mineking.discord.ui.modal.map
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.renderValue
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.GameStateHistory
import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.binary.BoardBinary
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.mutable
import de.mineking.hexo.board.render.notation.NotationType
import de.mineking.hexo.board.toGamePosition
import de.mineking.hexo.bot.CustomEmoji
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.main
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.asMediaGalleryItem
import de.mineking.hexo.bot.utils.effectiveLocale
import de.mineking.hexo.discord.core.DiscordUserId
import dev.freya02.jda.emojis.unicode.Emojis
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.Interaction

data class InteractiveBoardMenuParameter(val board: Board, val event: Interaction)

fun UIManager.boardMenu(
    notationMenu: MessageMenu<NotationMenuParameter, *>,
) = registerLocalizedMenu<InteractiveBoardMenuParameter, InteractiveBoardMenuLocalization>("board") {
    var reference by state(BoardReference.Binary(ByteArray(0)))
    val moveState = state(0)
    val showTurnNumbersState = state(false)

    initialize {
        reference = BoardReference.Binary(BoardBinary.encode(it.board))
        moveState.value = it.board.toGamePosition().numberOfStates
        showTurnNumbersState.value = it.board.attributes[BoardAttribute.ShowTurnNumbers] ?: false
    }

    val user = parameter({ DiscordUserId(0) }, { it.event.user.userId }, { user.userId })
    val locale = parameter({ DiscordLocale.UNKNOWN }, { it.event.effectiveLocale }, { event.effectiveLocale })
    localize(locale)

    val history = renderValue {
        val board = BoardBinary.decode(reference.data)
        board.toGamePosition()
    }

    +container {
        +interactiveBoard(
            main = main,
            notationMenu = notationMenu,
            reference = reference,
            history = history,
            moveState = moveState,
            showTurnNumbersState = showTurnNumbersState,
            locale = locale,
            user = user,
        )
    }
}

@OptIn(InternalBoardApi::class)
suspend fun MessageMenuConfig<*, *>.interactiveBoard(
    main: HeXODiscordBot,
    notationMenu: MessageMenu<NotationMenuParameter, *>,
    reference: BoardReference,
    history: GameStateHistory?,
    moveState: MutableState<Int>,
    showTurnNumbersState: MutableState<Boolean>,
    locale: DiscordLocale,
    user: DiscordUserId,
) = createMessageComponent<ContainerChildComponent> {
    val localization = menu.manager.manager.localizationManager.read<InteractiveBoardMenuLocalization>()

    var move by moveState

    val moveCount = history?.numberOfStates ?: Int.MAX_VALUE
    currentLocalizationConfig?.bindParameter("moveCount", moveCount)

    if (history != null) {
        move = move.coerceIn(1, moveCount)

        val theme = main.getUserTheme(user)
        val board = history.getState(move - 1).mutable().apply {
            focusWinningRows()
            attributes[BoardAttribute.ShowTurnNumbers] = showTurnNumbersState.value
        }

        main.run {
            +mediaGallery(board.asMediaGalleryItem(locale, theme))
            +separator(spacing = Separator.Spacing.LARGE)
        }
    }

    +moveSelector(localization, moveCount, moveState)

    +actionRow {
        +button(
            "notation",
            emoji = Emojis.PRINTER,
            localization = localization,
            label = "menu.board.notation.label".localize(),
        ) {
            replyMenu(notationMenu, NotationMenuParameter(reference, NotationType.CompactRectilinear, event), ephemeral = true).queue()
        }

        +toggleButton(
            "turn",
            emoji = main.emojiManager[if (showTurnNumbersState.value) CustomEmoji.SwitchOn else CustomEmoji.SwitchOff],
            localization = localization,
            label = "menu.board.turn.label".localize(),
            ref = showTurnNumbersState,
        ) { deferEdit().queue() }
    }
}.apply {
    // Explicitly resolve lazy component. This is required because the component executes code that has to run in all phases
    val _ = elements()
}

private fun MessageMenuConfig<*, *>.moveSelector(
    localization: LocalizationFile,
    max: Int,
    ref: MutableState<Int>,
): MessageComponent<ActionRow> {
    var move by ref

    return actionRow {
        +button("move-first", label = EmbedBuilder.ZERO_WIDTH_SPACE, emoji = Emojis.REWIND) {
            deferEdit().queue()
            move = 1
        }.disabledIf(move == 1)

        +button(
            "move-back",
            label = EmbedBuilder.ZERO_WIDTH_SPACE,
            emoji = Emojis.ARROW_LEFT,
        ) {
            deferEdit().queue()
            move--
        }.disabledIf(move <= 1)

        +modalButton(
            "move",
            label = "$move / $max",
            emoji = Emojis.PUZZLE_PIECE,
            localization = localization,
            title = "menu.board.move.title".localize(),
            component = localizedLabel(
                intInput(
                    "move",
                    value = move,
                    placeholder = "$move",
                ).unbox().map { it ?: terminateRender() },
                label = "menu.board.move.inputs.move.label".localize(),
                description = "menu.board.move.inputs.move.description".localize(),
            ),
        ) {
            deferEdit().queue()
            move = it.coerceIn(1, max)
        }

        +button(
            "move-next",
            label = EmbedBuilder.ZERO_WIDTH_SPACE,
            emoji = Emojis.ARROW_RIGHT,
        ) {
            deferEdit().queue()
            move++
        }.disabledIf(move >= max)

        +button("move-last", label = EmbedBuilder.ZERO_WIDTH_SPACE, emoji = Emojis.FAST_FORWARD) {
            deferEdit().queue()
            move = parameter()
        }.disabledIf(move == max).withParameter(max)
    }
}

interface InteractiveBoardMenuLocalization : LocalizationFile
