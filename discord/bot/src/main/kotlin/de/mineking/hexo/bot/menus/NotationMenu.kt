package de.mineking.hexo.bot.menus

import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.codeBlock
import de.mineking.discord.ui.builder.components.buildTextDisplay
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.actionRow
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.fileDisplay
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.builder.components.selectOption
import de.mineking.discord.ui.builder.components.stringSelect
import de.mineking.discord.ui.getValue
import de.mineking.discord.ui.initialize
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.parameter
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.render
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.hexo.board.binary.BoardBinary
import de.mineking.hexo.board.render.notation.NotationType
import de.mineking.hexo.board.render.render
import de.mineking.hexo.board.toBoard
import de.mineking.hexo.bot.CustomEmoji
import de.mineking.hexo.bot.main
import de.mineking.hexo.bot.utils.effectiveLocale
import de.mineking.hexo.game.model.game.FinishedGameRepository
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.utils.types.orElse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.components.separator.Separator.Spacing
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.utils.FileUpload

data class NotationMenuParameter(
    val reference: BoardReference,
    val notationType: NotationType,
    val event: Interaction,
)

@Serializable
sealed interface BoardReference {
    @Serializable
    @SerialName("game")
    data class Game(val gameId: GameId) : BoardReference

    @Serializable
    @SerialName("binary")
    class Binary(val data: ByteArray) : BoardReference
}

fun UIManager.notationMenu(
    gameRepository: FinishedGameRepository,
) = registerLocalizedMenu<NotationMenuParameter, NotationMenuLocalization>("notation") {
    var reference by state<BoardReference>(BoardReference.Game(GameId("")))
    var notationType by state<NotationType>(NotationType.CompactRectilinear)

    initialize {
        reference = it.reference
        notationType = it.notationType
    }

    val locale = parameter({ DiscordLocale.UNKNOWN }, { it.event.effectiveLocale }, { event.effectiveLocale })
    localize(locale)

    +container {
        +localizedTextDisplay("title")

        render {
            val board = when (val reference = reference) {
                is BoardReference.Game -> gameRepository.getGame(reference.gameId).orElse {
                    +localizedTextDisplay("game_not_found")
                    return@render
                }.position.toBoard()
                is BoardReference.Binary -> BoardBinary.decode(reference.data)
            }

            val notation = notationType.renderer.render(board)
            if (notation.length > Message.MAX_CONTENT_LENGTH_COMPONENT_V2 - 100) {
                +localizedTextDisplay("notation_too_long")
                +fileDisplay(FileUpload.fromData(notation.toByteArray(), "notation.txt"))
            } else {
                +buildTextDisplay {
                    +codeBlock("hexo", notation)
                }
            }
        }

        +separator(spacing = Spacing.LARGE)
        +localizedTextDisplay("notation_label")
        +actionRow(stringSelect("notation", placeholder = null, options = NotationType.entries.map {
            selectOption(it, default = it == notationType, emoji = when (it) {
                NotationType.CompactRectilinear -> main.emojiManager[CustomEmoji.NotationCRN]
                NotationType.MultilineRectilinear -> main.emojiManager[CustomEmoji.NotationMRN]
                NotationType.HTTTX -> main.emojiManager[CustomEmoji.NotationHTTTX]
                NotationType.Tyto -> main.emojiManager[CustomEmoji.NotationTyto]
            }) {
                notationType = it
            }
        }))
    }
}

interface NotationMenuLocalization : LocalizationFile
