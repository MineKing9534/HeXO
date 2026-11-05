package de.mineking.hexo.bot.commands

import de.mineking.discord.commands.localizedMessageCommand
import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.Localize
import de.mineking.discord.ui.UIManager
import de.mineking.discord.ui.builder.components.localizedTextDisplay
import de.mineking.discord.ui.builder.components.message.container
import de.mineking.discord.ui.builder.components.message.separator
import de.mineking.discord.ui.getValue
import de.mineking.discord.ui.initialize
import de.mineking.discord.ui.localize
import de.mineking.discord.ui.message.MessageMenu
import de.mineking.discord.ui.message.replyMenu
import de.mineking.discord.ui.registerLocalizedMenu
import de.mineking.discord.ui.renderValue
import de.mineking.discord.ui.setValue
import de.mineking.discord.ui.state
import de.mineking.discord.ui.virtualState
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.binary.BoardBinary
import de.mineking.hexo.board.recognition.BoardRecognitionException
import de.mineking.hexo.board.recognition.BoardRecognitionImageTooLargeException
import de.mineking.hexo.board.recognition.BoardRecognitionResult
import de.mineking.hexo.board.recognition.RecognitionWarning
import de.mineking.hexo.board.toGamePosition
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.main
import de.mineking.hexo.bot.menus.BoardReference
import de.mineking.hexo.bot.menus.NotationMenuParameter
import de.mineking.hexo.bot.menus.interactiveBoard
import de.mineking.hexo.bot.utils.finalErrorResponse
import de.mineking.hexo.bot.utils.localizationData
import de.mineking.hexo.bot.utils.recognitionImages
import de.mineking.hexo.bot.utils.recognizeImage
import kotlinx.coroutines.TimeoutCancellationException
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionContextType
import java.io.IOException

context(main: HeXODiscordBot)
fun recognizeBoardMessageCommand(
    notationMenu: MessageMenu<NotationMenuParameter, *>,
) = localizedMessageCommand<RecognizeBoardCommandLocalization>("recognizeMessage") { localization ->
    integrationTypes(IntegrationType.ALL)
    interactionContextTypes(InteractionContextType.ALL)

    val recognitionMenu = setup { get<UIManager>().recognitionMenu(notationMenu) }

    execute {
        val images = target.recognitionImages()

        val image = images.singleOrNull() ?: finalErrorResponse(localization.responseImageCount(userLocale))
        deferReply().queue()

        val result = try {
            main.boardRecognizer.recognizeImage(image)
        } catch (_: BoardRecognitionImageTooLargeException) {
            finalErrorResponse(localization.responseImageTooLarge(userLocale))
        } catch (_: IOException) {
            finalErrorResponse(localization.responseInvalidImage(userLocale))
        } catch (_: TimeoutCancellationException) {
            finalErrorResponse(localization.responseDownloadTimeout(userLocale))
        } catch (_: BoardRecognitionException) {
            finalErrorResponse(localization.responseRecognitionFailed(userLocale))
        }

        replyMenu(recognitionMenu, RecognitionMenuParameter(result, event)).queue()
    }
}

private data class RecognitionMenuParameter(val result: BoardRecognitionResult, val event: Interaction)

data class BoardRecognitionData(
    val board: Board,
    val confidence: Double,
    val warnings: Set<RecognitionWarning>,
)

private fun UIManager.recognitionMenu(
    notationMenu: MessageMenu<NotationMenuParameter, *>,
) = registerLocalizedMenu<RecognitionMenuParameter, RecognizeBoardCommandLocalization>("recognition") { localization ->

    var reference by state(BoardReference.Binary(ByteArray(0)))
    var confidence by state(0.0)
    var warnings by state(emptySet<RecognitionWarning>())

    initialize {
        reference = BoardReference.Binary(BoardBinary.encode(it.result.board))
        confidence = it.result.confidence
        warnings = it.result.warnings
    }

    val (user, locale) = localizationData { it.event }

    val decoded = renderValue { BoardBinary.decode(reference.data) }

    localize(locale) {
        if (decoded != null) {
            bindParameter("data", BoardRecognitionData(decoded, confidence, warnings))
        }
    }

    +container {
        +localizedTextDisplay("summary")
        +separator(spacing = Separator.Spacing.LARGE)

        +interactiveBoard(
            main = main,
            notationMenu = notationMenu,
            reference = reference,
            history = decoded?.toGamePosition(),
            moveState = virtualState(0),
            showTurnNumbersState = null,
            locale = locale,
            user = user,
        )
    }
}

interface RecognizeBoardCommandLocalization : LocalizationFile {
    @Localize
    fun responseImageCount(@Locale locale: DiscordLocale): String

    @Localize
    fun responseInvalidImage(@Locale locale: DiscordLocale): String

    @Localize
    fun responseImageTooLarge(@Locale locale: DiscordLocale): String

    @Localize
    fun responseDownloadTimeout(@Locale locale: DiscordLocale): String

    @Localize
    fun responseRecognitionFailed(@Locale locale: DiscordLocale): String
}
