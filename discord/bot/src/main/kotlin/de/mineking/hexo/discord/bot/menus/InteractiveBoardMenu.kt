package de.mineking.hexo.discord.bot.menus

import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.components.LabelLocalization
import de.mineking.discord.extensions.components.label
import de.mineking.discord.extensions.components.neutralSwitch
import de.mineking.discord.extensions.localization.localizeForUser
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.Defer
import de.mineking.discord.menu.action
import de.mineking.discord.menu.components.onlyIf
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.discord.menu.message.components.MessageComponentScope
import de.mineking.discord.menu.message.components.actionRow
import de.mineking.discord.menu.message.components.button
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.message.components.linkButton
import de.mineking.discord.menu.message.components.mediaGallery
import de.mineking.discord.menu.message.components.modalButton
import de.mineking.discord.menu.message.components.separator
import de.mineking.discord.menu.message.components.switchMenuButton
import de.mineking.discord.menu.modal.components.textInput
import de.mineking.discord.menu.modal.modalValue
import de.mineking.discord.menu.renderOrElse
import de.mineking.discord.menu.runtime.MenuRuntime
import de.mineking.discord.menu.state.MutableState
import de.mineking.discord.menu.state.ref
import de.mineking.discord.menu.state.state
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.GameStateHistory
import de.mineking.hexo.board.InternalBoardApi
import de.mineking.hexo.board.binary.decode
import de.mineking.hexo.board.binary.encodeBinary
import de.mineking.hexo.board.focusWinningRows
import de.mineking.hexo.board.mutable
import de.mineking.hexo.board.toGamePosition
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.IconEmojis
import de.mineking.hexo.discord.bot.throwValidationError
import de.mineking.hexo.discord.bot.userId
import de.mineking.hexo.discord.bot.utils.BoardReference
import de.mineking.hexo.discord.bot.utils.NotationType
import de.mineking.hexo.discord.bot.utils.boardImage
import dev.freya02.jda.emojis.unicode.Emojis
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.separator.Separator

context(_: DiscordToolKitContext<HeXODiscordBot>)
fun MenuRuntime.boardMenu(notationMenu: MessageMenu<NotationMenuParameter>) = registerMessageMenu<Board>("board") {
    localizeForUser()

    var reference by state(fromInput { BoardReference.Binary(input.encodeBinary()) })
    val move by state(fromInput { input.toGamePosition().numberOfStates })
    val showTurnNumbers by state(fromInput { input.attributes[BoardAttribute.ShowTurnNumbers] ?: false })

    val history = renderOrElse(null) {
        val board = reference.data.decode()
        board.toGamePosition()
    }

    container {
        interactiveBoard(
            notationMenu = notationMenu,
            reference = reference,
            history = history,
            moveState = move.ref,
            showTurnNumbersState = showTurnNumbers.ref,
        )
    }
}

@OptIn(InternalBoardApi::class)
context(context: DiscordToolKitContext<HeXODiscordBot>)
suspend fun MessageComponentScope<ContainerChildComponent>.interactiveBoard(
    notationMenu: MessageMenu<NotationMenuParameter>,
    reference: BoardReference,
    history: GameStateHistory?,
    moveState: MutableState<Int>,
    showTurnNumbersState: MutableState<Boolean>,
) {
    val localization = translation<InteractiveBoardMenuLocalization>()

    var move by moveState
    val moveCount = history?.numberOfStates ?: Int.MAX_VALUE

    val visibleBoard = history?.let {
        move = move.coerceIn(1, moveCount)
        it.getState(move - 1).mutable().apply {
            focusWinningRows()
            attributes[BoardAttribute.ShowTurnNumbers] = showTurnNumbersState.value
        }
    }

    if (visibleBoard != null) {
        val theme = context.main.getUserTheme(requireInteraction().user.userId)
        mediaGallery(boardImage(visibleBoard, theme))
        separator(Separator.Spacing.LARGE)
    }

    moveSelector(moveState, max = moveCount)
    actionRow {
        switchMenuButton(
            notationMenu,
            defer = Defer.Reply(true),
            label = localization.showNotation(),
            emoji = Emojis.PRINTER,
        ) {
            NotationMenuParameter(reference, NotationType.CompactRectilinear)
        }

        neutralSwitch(showTurnNumbersState, label = localization.showTurnNumbers())
    }

    actionRow {
        visibleBoard?.sandboxUrl(context.main.publicUrl)?.let { url ->
            linkButton(
                url = url,
                label = localization.exploreLabel(),
                emoji = Emojis.TEST_TUBE,
            )
        }
    }
}

context(context: DiscordToolKitContext<*>)
private suspend fun MessageComponentScope<ActionRow>.moveSelector(
    state: MutableState<Int>,
    max: Int,
) {
    var move by state

    val action = action<Int>("move") {
        move = it
    }

    actionRow {
        button(action.bind(1), label = "", emoji = context.emojis[IconEmojis.First], disabled = onlyIf(move == 1))
        button(action.bind(move - 1), label = "", emoji = context.emojis[IconEmojis.Previous], disabled = onlyIf(move <= 1))

        modalButton("set-move", label = "$move / $max", emoji = context.emojis[IconEmojis.Dialog]) {
            val localization = translation<InteractiveBoardMenuLocalization>()
            title(localization.moveSelectTitle())

            val value = modalValue<String> {
                label(localization.moveSelectLabel(max)) {
                    textInput(it.bind(), value = "$move", placeholder = "$move")
                }
            }

            onSubmit {
                val value = value.value().toIntOrNull()
                    ?: localization.moveInvalid().throwValidationError()

                move = value.coerceIn(1, max)
            }
        }

        button(action.bind(move + 1), label = "", emoji = context.emojis[IconEmojis.Next], disabled = onlyIf(move >= max))
        button(action.bind(max), label = "", emoji = context.emojis[IconEmojis.Last], disabled = onlyIf(move == max))
    }
}

internal fun Board.sandboxUrl(baseUrl: String?): String? {
    if (baseUrl.isNullOrBlank()) return null
    val base = baseUrl.trimEnd('/')
    val url = "$base/sandbox?position=${encodeBinary()}"

    return url.takeIf { it.length <= Button.URL_MAX_LENGTH }
}

@LocalizationBundle("board")
internal interface InteractiveBoardMenuLocalization {
    fun showNotation(): String
    fun showTurnNumbers(): String

    fun moveSelectTitle(): String
    fun moveSelectLabel(parameter: Int): LabelLocalization<Int>

    fun moveInvalid(): ErrorResponseMessage<Unit>

    fun exploreLabel(): String
}
