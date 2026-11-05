package de.mineking.hexo.discord.bot.menus

import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.localize
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.localization.LocalizationMessageGroup
import de.mineking.discord.localization.LocalizationOperation
import de.mineking.discord.menu.components.ComponentScope
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.message.components.section
import de.mineking.discord.menu.message.components.separator
import de.mineking.discord.menu.renderOrElse
import de.mineking.discord.menu.runtime.MenuRuntime
import de.mineking.discord.menu.state.ref
import de.mineking.discord.menu.state.state
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.PlayerEmojis
import de.mineking.hexo.discord.bot.utils.BoardReference
import de.mineking.hexo.discord.bot.utils.viewButton
import de.mineking.hexo.game.model.TimeControl
import de.mineking.hexo.game.model.game.FinishedGame
import de.mineking.hexo.game.model.game.GameFinishReason
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.utils.types.orElse
import dev.freya02.jda.emojis.unicode.Emojis
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class GameMenuParameter(val id: GameId, val move: Int)

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun MenuRuntime.gameMenu(notationMenu: MessageMenu<NotationMenuParameter>) = registerMessageMenu<GameMenuParameter>("game") {
    localize()
    val localization = translation<GameMenuLocalization>()

    val id by state<GameId>(fromInput { input.id })
    val move by state<Int>(fromInput { input.move })
    val showTurnNumbers by state(false)

    val game = renderOrElse(null) {
        context.main.repositories.finishedGameRepository.getGame(id).orElse {
            localization.gameNotFound().throwValidationError(id.value)
        }
    }

    container {
        if (game != null) {
            gameTitle(game)
            gameDetails(game)

            separator(Separator.Spacing.LARGE)
        }

        interactiveBoard(
            notationMenu = notationMenu,
            reference = BoardReference.Game(id, move),
            history = game?.position,
            moveState = move.ref,
            showTurnNumbersState = showTurnNumbers.ref,
        )
    }
}

context(_: DiscordToolKitContext<*>)
private suspend fun ComponentScope<Section>.gameTitle(game: FinishedGame) {
    val localization = translation<GameMenuLocalization>()
    section(accessory = {
        viewButton(game.url)
    }) {
        text {
            h1 {
                text(when {
                    game.tournament != null -> ":dart:"
                    game.options.rated -> ":trophy:"
                    else -> ":seedling:"
                })
                text(" ${localization.title()}")
            }

            val tournament = game.tournament
            if (tournament != null) {
                subtext {
                    text("${localization.tournament()}: ")
                    link(tournament.tournament.info.name, url = tournament.tournament.info.url)

                    if (tournament.matchInfo.bestOf > 1) {
                        text(", G${tournament.matchInfo.currentGameNumber} (BO${tournament.matchInfo.bestOf})")
                    }
                }
            }
        }
    }
}

context(_: DiscordToolKitContext<*>)
private fun ComponentScope<TextDisplay>.gameDetails(game: FinishedGame) {
    val localization = translation<GameMenuLocalization>()

    text {
        game.players.forEach { player ->
            line {
                raw(player.color.emoji.formatted)
                text(" ${player.displayName}")

                player.elo?.let { elo ->
                    val eloChange = player.eloChange
                        ?.let { "　[${if (it < 0) "▼" else "▲"} ${it.absoluteValue}]" }
                        .orEmpty()
                    raw("　`$elo ELO$eloChange`")
                }
                if (game.result.winner?.id == player.id) raw(" :first_place:")
            }
        }

        line()
        line {
            code("${Emojis.TIMER_CLOCK.formatted} ${game.result.duration.inWholeSeconds.seconds}")
            text("\u2003")
            code("${Emojis.HOURGLASS.formatted} ${localization.timeControl().format(game.options.timeControl)}")
            text("\u2003")
            code(game.result.reason.localize(localization))
        }
    }
}

context(context: DiscordToolKitContext<*>)
private val CellOwner.emoji get() = context.emojis[when (this) {
    CellOwner.X -> PlayerEmojis.X
    CellOwner.O -> PlayerEmojis.O
},]

private fun GameFinishReason.localize(localization: GameMenuLocalization): String {
    val emoji = when (this) {
        is GameFinishReason.Regular -> Emojis.TROPHY
        is GameFinishReason.Timeout -> Emojis.ALARM_CLOCK
        is GameFinishReason.Surrender -> Emojis.FLAG_WHITE
        is GameFinishReason.Disconnect -> Emojis.ELECTRIC_PLUG
        is GameFinishReason.DrawAgreement -> Emojis.HANDSHAKE
        is GameFinishReason.Terminated -> Emojis.WARNING
        is GameFinishReason.Aborted -> Emojis.NO_ENTRY
    }

    return "${emoji.formatted} ${localization.finishReason().format(this)}"
}

@LocalizationBundle("game")
internal interface GameMenuLocalization {
    fun title(): String
    fun tournament(): String

    @LocalizationMessageGroup
    interface FinishReasonLocalization {
        fun regular(length: Int): String
        fun timeout(): String
        fun surrender(): String
        fun disconnect(): String
        fun drawAgreement(): String
        fun terminated(): String
        fun aborted(abortingPlayerName: String): String

        @LocalizationOperation
        fun format(reason: GameFinishReason) = when (reason) {
            is GameFinishReason.Regular -> regular(reason.length)
            is GameFinishReason.Timeout -> timeout()
            is GameFinishReason.Surrender -> surrender()
            is GameFinishReason.Disconnect -> disconnect()
            is GameFinishReason.DrawAgreement -> drawAgreement()
            is GameFinishReason.Terminated -> terminated()
            is GameFinishReason.Aborted -> aborted(reason.abortingPlayer.displayName)
        }
    }

    fun finishReason(): FinishReasonLocalization

    @LocalizationMessageGroup
    interface TimeControlLocalization {
        fun unlimited(): String
        fun match(main: Duration, increment: Duration): String
        fun turn(time: Duration): String

        @LocalizationOperation
        fun format(timeControl: TimeControl) = when (timeControl) {
            is TimeControl.Unlimited -> unlimited()
            is TimeControl.Match -> match(timeControl.mainTime, timeControl.increment)
            is TimeControl.Turn -> turn(timeControl.turnTime)
        }
    }

    fun timeControl(): TimeControlLocalization

    fun gameNotFound(): ErrorResponseMessage<String>
}
