package de.mineking.hexo.bot.menus

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.MutableState
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.append
import de.mineking.discord.ui.builder.code
import de.mineking.discord.ui.builder.components.buildTextDisplay
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.actionRow
import de.mineking.discord.ui.builder.components.message.button
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.link
import de.mineking.discord.ui.builder.components.message.mediaGallery
import de.mineking.discord.ui.builder.components.message.modalButton
import de.mineking.discord.ui.builder.components.message.section
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.message.toggleButton
import de.mineking.discord.ui.builder.components.modal.intInput
import de.mineking.discord.ui.builder.components.modal.localizedLabel
import de.mineking.discord.ui.builder.components.modal.unbox
import de.mineking.discord.ui.builder.line
import de.mineking.discord.ui.disabledIf
import de.mineking.discord.ui.getValue
import de.mineking.discord.ui.initialize
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.MessageComponent
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.MessageMenuConfig
import de.mineking.discord.ui.message.parameter
import de.mineking.discord.ui.message.replyMenu
import de.mineking.discord.ui.message.withParameter
import de.mineking.discord.ui.modal.map
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.render
import de.mineking.discord.ui.renderValue
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.BoardAttribute
import de.mineking.hexo.board.BoardAttributes
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.board.moves
import de.mineking.hexo.board.render.notation.NotationType
import de.mineking.hexo.board.take
import de.mineking.hexo.board.to
import de.mineking.hexo.board.toBoard
import de.mineking.hexo.bot.CustomEmoji
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.main
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.MessageColor
import de.mineking.hexo.bot.utils.asMediaGalleryItem
import de.mineking.hexo.bot.utils.effectiveLocale
import de.mineking.hexo.bot.utils.respond
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.FinishedGameWithPosition
import de.mineking.hexo.game.model.game.GameFinishReason
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.game.isGuest
import de.mineking.hexo.utils.types.orElse
import dev.freya02.jda.emojis.unicode.Emojis
import net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

data class GameMenuParameter(val event: IReplyCallback, val id: GameId, val move: Int)

fun UIManager.gameMenu(
    gameRepository: FinishedGameRepository,
    notationMenu: MessageMenu<NotationMenuParameter, *>,
) = registerLocalizedMenu<GameMenuParameter, GameMenuLocalization>("game") { localization ->
    var id by state(GameId(""))
    val moveState = state(0)
    val showTurnNumbers = state(false)

    var move by moveState

    initialize {
        id = it.id
        move = it.move
    }

    val user = parameter({ DiscordUserId(0) }, { it.event.user.userId }, { user.userId })
    val locale = parameter({ DiscordLocale.UNKNOWN }, { it.event.effectiveLocale }, { event.effectiveLocale })
    localize(locale) // Predefine locale for potential error handling

    data class GameData(val game: FinishedGameWithPosition, val board: Board)
    val gameData = renderValue {
        val event = parameter({ error("") }, { it.event }, { event })
        val game = gameRepository.getGame(id).orElse {
            event.respond(MessageColor.Error, localization.errorMatchNotFound(event.effectiveLocale, id))
            terminateRender()
        }

        val board = game.position.take(move).toBoard(
            focusWinningRows = true,
            attributes = BoardAttributes(BoardAttribute.ShowTurnNumbers to showTurnNumbers.value),
        )

        move = move.coerceIn(0, game.position.moves.size)
        localize(locale) {
            bindParameter("game", game)
        }

        GameData(game, board)
    }

    +container {
        render {
            val (game, board) = gameData ?: return@render
            val theme = main.getUserTheme(user)

            +section(
                accessory = link("view", emoji = Emojis.GLOBE_WITH_MERIDIANS, url = game.url),
                localizedTextDisplay("title"),
            )
            +separator(invisible = true)

            main.run {
                +game.gameDetails(localization, locale)

                +separator(spacing = Separator.Spacing.LARGE)
                +mediaGallery(board.asMediaGalleryItem(theme))
                +separator(spacing = Separator.Spacing.LARGE)
            }
        }

        +moveSelector("move", gameData?.game?.position?.moves?.size ?: Int.MAX_VALUE, moveState)
        +additionalActions(main, id, notationMenu, showTurnNumbers)
    }
}

context(main: HeXODiscordBot)
private fun FinishedGame.gameDetails(localization: GameMenuLocalization, locale: DiscordLocale) = buildTextDisplay {
    players.forEach { player ->
        +line {
            val emoji = when (player.color) {
                CellOwner.X -> CustomEmoji.PlayerX
                CellOwner.O -> CustomEmoji.PlayerO
            }

            append(main.emojiManager[emoji].formatted)
            append(" ")
            append(player.displayName)

            if (!player.isGuest()) {
                val eloChange = player.eloChange?.let { "　[${if (it < 0) "▼" else "▲"} ${it.absoluteValue}]" } ?: ""
                append("　`${player.elo} ELO$eloChange`")
            }
            if (result.winner?.playerId == player.playerId) append(" :first_place:")
        }
    }

    +line()
    +line {
        +code("${Emojis.TIMER_CLOCK.formatted} ${result.duration.inWholeSeconds.seconds}")
        append("\u2003")
        +code("${Emojis.HOURGLASS.formatted} ${localization.timeControl(locale, options.timeControl)}")
        append("\u2003")
        +code(result.reason.localize(locale, localization))
    }
}

private fun additionalActions(
    main: HeXODiscordBot,
    gameId: GameId,
    notationMenu: MessageMenu<NotationMenuParameter, *>,
    showTurnNumber: MutableState<Boolean>,
) = actionRow {
    +notationButton(gameId, notationMenu)
    +toggleButton(
        "turn",
        emoji = main.emojiManager[if (showTurnNumber.value) CustomEmoji.SwitchOn else CustomEmoji.SwitchOff],
        ref = showTurnNumber,
    ) { deferEdit().queue() }
}

private fun notationButton(
    gameId: GameId,
    notationMenu: MessageMenu<NotationMenuParameter, *>,
) = button(
    "notation",
    emoji = Emojis.PRINTER,
) {
    replyMenu(notationMenu, NotationMenuParameter(gameId, NotationType.CompactRectilinear, event), ephemeral = true).queue()
}

private fun GameFinishReason.localize(locale: DiscordLocale, localization: GameMenuLocalization): String {
    val emoji = when (this) {
        is GameFinishReason.Regular -> Emojis.TRIANGULAR_RULER
        is GameFinishReason.Timeout -> Emojis.ALARM_CLOCK
        is GameFinishReason.Surrender -> Emojis.FLAG_WHITE
        is GameFinishReason.Disconnect -> Emojis.SATELLITE
        is GameFinishReason.DrawAgreement -> Emojis.HANDSHAKE
        is GameFinishReason.Terminated -> Emojis.NO_ENTRY
    }

    return "${emoji.formatted} ${localization.finishReason(locale, this)}"
}

private fun MessageMenuConfig<*, *>.moveSelector(
    name: String,
    max: Int,
    ref: MutableState<Int>,
): MessageComponent<ActionRow> {
    var move by ref

    return actionRow {
        +button("$name-first", label = ZERO_WIDTH_SPACE, emoji = Emojis.REWIND) {
            deferEdit().queue()
            move = 1
        }.disabledIf(move == 1)

        +button(
            "$name-back",
            label = ZERO_WIDTH_SPACE,
            emoji = Emojis.ARROW_LEFT,
        ) {
            deferEdit().queue()
            move--
        }.disabledIf(move <= 1)

        +modalButton(
            name,
            label = "$move / $max",
            emoji = Emojis.PUZZLE_PIECE,
            component = localizedLabel(
                intInput(
                    "move",
                    value = move,
                    placeholder = "$move",
                ).unbox().map { it ?: terminateRender() },
            ),
        ) {
            deferEdit().queue()
            move = it.coerceIn(1, max)
        }

        +button(
            "$name-next",
            label = ZERO_WIDTH_SPACE,
            emoji = Emojis.ARROW_RIGHT,
        ) {
            deferEdit().queue()
            move++
        }.disabledIf(move >= max)

        +button("$name-last", label = ZERO_WIDTH_SPACE, emoji = Emojis.FAST_FORWARD) {
            deferEdit().queue()
            move = parameter()
        }.disabledIf(move == max).withParameter(max)
    }
}

interface GameMenuLocalization : LocalizationFile {
    @Localize
    fun errorMatchNotFound(@Locale locale: DiscordLocale, @LocalizationParameter id: GameId): String

    @Localize
    fun timeControl(@Locale locale: DiscordLocale, @LocalizationParameter control: TimeControl): String

    @Localize
    fun finishReason(@Locale locale: DiscordLocale, @LocalizationParameter reason: GameFinishReason): String
}
