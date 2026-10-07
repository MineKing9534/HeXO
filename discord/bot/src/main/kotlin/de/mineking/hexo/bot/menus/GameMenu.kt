package de.mineking.hexo.bot.menus

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.LocalizationParameter
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.append
import de.mineking.discord.ui.builder.code
import de.mineking.discord.ui.builder.components.buildTextDisplay
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.link
import de.mineking.discord.ui.builder.components.message.section
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.line
import de.mineking.discord.ui.getValue
import de.mineking.discord.ui.initialize
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.render
import de.mineking.discord.ui.renderValue
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.discord.ui.terminateRender
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.bot.CustomEmoji
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.main
import de.mineking.hexo.bot.userId
import de.mineking.hexo.bot.utils.MessageColor
import de.mineking.hexo.bot.utils.effectiveLocale
import de.mineking.hexo.bot.utils.respond
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.GameFinishReason
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.utils.types.orElse
import dev.freya02.jda.emojis.unicode.Emojis
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
    val showTurnNumbersState = state(false)

    initialize {
        id = it.id
        moveState.value = it.move
    }

    val user = parameter({ DiscordUserId(0) }, { it.event.user.userId }, { user.userId })
    val locale = parameter({ DiscordLocale.UNKNOWN }, { it.event.effectiveLocale }, { event.effectiveLocale })
    localize(locale) // Predefine locale for potential error handling

    val game = renderValue {
        val event = parameter({ error("") }, { it.event }, { event })
        val game = gameRepository.getGame(id).orElse {
            event.respond(MessageColor.Error, localization.errorMatchNotFound(event.effectiveLocale, id))
            terminateRender()
        }

        localize(locale) {
            bindParameter("game", game)
        }

        game
    }

    +container {
        render {
            val game = game ?: return@render

            +section(
                accessory = link("view", emoji = Emojis.GLOBE_WITH_MERIDIANS, url = game.url),
                localizedTextDisplay("title"),
            )
            +separator(invisible = true)

            main.run {
                +game.gameDetails(localization, locale)
            }

            +separator(spacing = Separator.Spacing.LARGE)
        }

        +interactiveBoard(
            main = main,
            notationMenu = notationMenu,
            reference = BoardReference.Game(id),
            history = game?.position,
            moveState = moveState,
            showTurnNumbersState = showTurnNumbersState,
            locale = locale,
            user = user,
        )
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

            player.elo?.let { elo ->
                val eloChange = player.eloChange?.let { "　[${if (it < 0) "▼" else "▲"} ${it.absoluteValue}]" } ?: ""
                append("　`$elo ELO$eloChange`")
            }
            if (result.winner?.id == player.id) append(" :first_place:")
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

private fun GameFinishReason.localize(locale: DiscordLocale, localization: GameMenuLocalization): String {
    val emoji = when (this) {
        is GameFinishReason.Regular -> Emojis.TROPHY
        is GameFinishReason.Timeout -> Emojis.ALARM_CLOCK
        is GameFinishReason.Surrender -> Emojis.FLAG_WHITE
        is GameFinishReason.Disconnect -> Emojis.ELECTRIC_PLUG
        is GameFinishReason.DrawAgreement -> Emojis.HANDSHAKE
        is GameFinishReason.Terminated -> Emojis.WARNING
        is GameFinishReason.Aborted -> Emojis.NO_ENTRY
    }

    return "${emoji.formatted} ${localization.finishReason(locale, this)}"
}

interface GameMenuLocalization : LocalizationFile {
    @Localize
    fun errorMatchNotFound(@Locale locale: DiscordLocale, @LocalizationParameter id: GameId): String

    @Localize
    fun timeControl(@Locale locale: DiscordLocale, @LocalizationParameter control: TimeControl): String

    @Localize
    fun finishReason(@Locale locale: DiscordLocale, @LocalizationParameter reason: GameFinishReason): String
}
