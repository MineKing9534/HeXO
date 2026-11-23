package de.mineking.hexo.discord.bot.commands

import de.mineking.discord.await
import de.mineking.discord.command.ReplyMode
import de.mineking.discord.command.RootCommandRegistrationScope
import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.components.stringSelect
import de.mineking.discord.extensions.localization.localizeForUser
import de.mineking.discord.extensions.localization.localizedWith
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.action
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.MessageMenu
import de.mineking.discord.menu.message.components.actionRow
import de.mineking.discord.menu.message.components.button
import de.mineking.discord.menu.message.components.container
import de.mineking.discord.menu.message.components.mediaGallery
import de.mineking.discord.menu.message.components.separator
import de.mineking.discord.menu.renderOrElse
import de.mineking.discord.menu.runtime.MenuRuntime
import de.mineking.discord.menu.state.ref
import de.mineking.discord.menu.state.state
import de.mineking.hexo.board.binary.decode
import de.mineking.hexo.board.binary.encodeBinary
import de.mineking.hexo.board.recognition.BoardRecognitionException
import de.mineking.hexo.board.recognition.BoardRecognitionImageTooLargeException
import de.mineking.hexo.board.recognition.BoardRecognitionResult
import de.mineking.hexo.board.recognition.RecognitionWarning
import de.mineking.hexo.board.toGamePosition
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.IconEmojis
import de.mineking.hexo.discord.bot.menus.NotationMenuParameter
import de.mineking.hexo.discord.bot.menus.interactiveBoard
import de.mineking.hexo.discord.bot.throwValidationError
import de.mineking.hexo.discord.bot.utils.BoardReference
import de.mineking.hexo.discord.bot.utils.recognitionImages
import de.mineking.hexo.discord.bot.utils.recognizeImage
import de.mineking.hexo.utils.types.orElse
import kotlinx.coroutines.TimeoutCancellationException
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.utils.FileProxy
import java.io.IOException
import kotlin.math.roundToInt

context(context: DiscordToolKitContext<HeXODiscordBot>)
fun RootCommandRegistrationScope.recognizeBoardMessageCommand(
    notationMenu: MessageMenu<NotationMenuParameter>,
) = localizedWith<RecognizeBoardCommandLocalization> {
    messageCommand(localized { name() }) {
        val recognitionMenu = context.menuRuntime.recognitionMenu(notationMenu)
        val selectImageMenu = context.menuRuntime.imageSelectMenu(recognitionMenu)

        execute(ReplyMode.Defer()) {
            val localization = bindForUser()

            val images = interaction.target.recognitionImages()
            when (images.size) {
                0 -> localization.noImage().throwValidationError()
                1 -> {
                    val result = recognize(localization, images.single())
                    replyMenu(recognitionMenu, result)
                }
                else -> replyMenu(selectImageMenu, interaction.target.idLong)
            }
        }
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private suspend fun recognize(
    localization: RecognizeBoardCommandLocalization,
    proxy: FileProxy,
): BoardRecognitionResult {
    return try {
        context.main.boardRecognizer.recognizeImage(proxy).orElse {
            localization.recognitionFailed().throwValidationError()
        }
    } catch (_: BoardRecognitionImageTooLargeException) {
        localization.imageTooLarge().throwValidationError()
    } catch (_: IOException) {
        localization.invalidImage().throwValidationError()
    } catch (_: TimeoutCancellationException) {
        localization.downloadTimeout().throwValidationError()
    } catch (_: BoardRecognitionException) {
        localization.recognitionFailed().throwValidationError()
    }
}

context(context: DiscordToolKitContext<HeXODiscordBot>)
private fun MenuRuntime.imageSelectMenu(
    recognitionMenu: MessageMenu<BoardRecognitionResult>,
) = registerMessageMenu<Long>("imageSelect") {
    localizeForUser()
    val localization = translation<RecognizeBoardCommandLocalization>()

    val messageId by state(fromInput { input })
    var selected by state(0)

    val message = requireInteraction().messageChannel
        .retrieveMessageById(messageId)
        .await()
    val images = message.recognitionImages()

    container {
        mediaGallery()

        separator(Separator.Spacing.LARGE)
        actionRow {
            stringSelect<_, Int>(action<_, _>("select") {
                selected = it.single()
            }) {
                images.forEachIndexed { index, proxy ->
                    option(
                        value = index,
                        label = localization.imageLabel(index + 1),
                        description = proxy.url.ellipsize(SelectOption.DESCRIPTION_MAX_LENGTH),
                    )
                }
            }
        }

        actionRow {
            button(action("recognize") {
                val result = recognize(localization, images[selected])
                replaceMenu(recognitionMenu, result)
            }, emoji = context.emojis[IconEmojis.Scan], label = localization.scanLabel())
        }
    }
}

context(_: DiscordToolKitContext<HeXODiscordBot>)
private fun MenuRuntime.recognitionMenu(
    notationMenu: MessageMenu<NotationMenuParameter>,
) = registerMessageMenu<BoardRecognitionResult>("recognition") {
    localizeForUser()
    val localization = translation<RecognizeBoardCommandLocalization>()

    val move by state(0)
    val reference by state(fromInput { BoardReference.Binary(input.board.encodeBinary()) })
    val confidence by state(fromInput { input.confidence })
    val warnings by state(fromInput { input.warnings })

    val decoded = renderOrElse(null) { reference.data.decode() }

    container {
        text {
            fun summary(label: String, value: String) = line {
                bold("$label: ")
                text(value)
            }

            h1(":jigsaw: ${localization.title()}")
            summary(localization.confidence(), "${(confidence * 100).roundToInt()}%")
            summary(localization.cells(), "${decoded?.cells?.size}")

            if (warnings.isNotEmpty()) {
                line()
                h3(":warning:  ${localization.warnings()}")
                warnings.forEach {
                    line(localization.warning(it))
                }
            }
        }

        separator(Separator.Spacing.LARGE)
        interactiveBoard(
            notationMenu = notationMenu,
            reference = reference,
            history = decoded?.toGamePosition(),
            moveState = move.ref,
            showTurnNumbersState = null,
        )
    }
}

private fun String.ellipsize(maxLength: Int): String {
    if (length < maxLength) return this
    return substring(0, maxLength - 3) + "..."
}

@LocalizationBundle("recognizeBoard")
interface RecognizeBoardCommandLocalization {
    fun name(): String

    fun title(): String
    fun confidence(): String
    fun cells(): String
    fun warnings(): String
    fun warning(warning: RecognitionWarning): String

    fun imageLabel(position: Int): String
    fun scanLabel(): String

    fun noImage(): ErrorResponseMessage<Unit>
    fun imageTooLarge(): ErrorResponseMessage<Unit>
    fun invalidImage(): ErrorResponseMessage<Unit>
    fun downloadTimeout(): ErrorResponseMessage<Unit>
    fun recognitionFailed(): ErrorResponseMessage<Unit>
}
