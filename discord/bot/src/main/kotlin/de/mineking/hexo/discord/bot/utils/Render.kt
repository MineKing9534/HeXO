package de.mineking.hexo.discord.bot.utils

import de.mineking.discord.extensions.DiscordToolKitContext
import de.mineking.discord.extensions.localization.locale
import de.mineking.discord.extensions.localization.translation
import de.mineking.discord.localization.LocalizationBundle
import de.mineking.discord.menu.MenuEnvironment
import de.mineking.discord.menu.components.ComponentScope
import de.mineking.discord.menu.components.text
import de.mineking.discord.menu.message.components.mediaGallery
import de.mineking.discord.menu.message.components.separator
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.discord.bot.DiscordLocalization
import de.mineking.hexo.discord.bot.ErrorResponseMessage
import de.mineking.hexo.discord.bot.HeXODiscordBot
import de.mineking.hexo.discord.bot.grammar.CompositeHexoLexer
import de.mineking.hexo.discord.bot.grammar.CompositeHexoParser
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.components.MessageContentComponent
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.ParserRuleContext

context(context: DiscordToolKitContext<HeXODiscordBot>)
suspend fun MenuEnvironment.boardImage(board: Board, theme: Theme): MediaGalleryItem {
    return withContext(DiscordLocalization(context.translationManager, locale)) {
        context.main.boardRenderer.render(board, theme).toMediaGalleryItem()
    }
}

object MissingImages : IError

@IgnorableReturnValue
context(context: DiscordToolKitContext<HeXODiscordBot>)
suspend fun ComponentScope<MessageContentComponent>.compositeHexoNotation(content: String, theme: Theme): Result<Unit, MissingImages> {
    var hasImage = false
    val components = mutableListOf<MessageContentComponent>()

    val scope = object : ComponentScope<MessageContentComponent> by this {
        override fun component(component: MessageContentComponent) {
            if (component is MediaGallery) {
                hasImage = true
            }
            components += component
        }
    }

    CompositeNotationRenderer(scope) { notation, isParagraph ->
        try {
            val board = context.main.notationParser.parse(notation.trim())
            boardImage(board, theme).withDescription(notation.takeIf { isParagraph })
        } catch (_: HexoNotationException) {
            null
        }
    }.render(content)

    if (hasImage) {
        components.forEach { component(it) }
        return Result.Success(Unit)
    } else {
        val localization = translation<RenderLocalization>()
        text(localization.missingImage())

        return Result.Error(MissingImages)
    }
}

private class CompositeNotationRenderer(
    private val scope: ComponentScope<MessageContentComponent>,
    private val renderImage: suspend (String, Boolean) -> MediaGalleryItem?,
) {
    private val buffer = NotationTextBuffer()
    private val images = mutableListOf<MediaGalleryItem>()
    private var lastWasGallery = false

    suspend fun render(content: String) {
        val lexer = CompositeHexoLexer(CharStreams.fromString(content))
        visit(CompositeHexoParser(CommonTokenStream(lexer)).message())
        flush(endOfMessage = true)
    }

    private suspend fun visit(node: ParserRuleContext) {
        when (node) {
            is CompositeHexoParser.CodeBlockContext -> renderBlock(node.text)
            is CompositeHexoParser.InlineCodeContext -> renderInline(node.text, node.text.inlineNotation())
            is CompositeHexoParser.UrlContext -> renderInline(node.text, node.text.notationUrl())
            is CompositeHexoParser.SpoilerContext -> renderSpoiler(node)
            is CompositeHexoParser.ParagraphBreakContext -> {
                buffer.append(node.text)
                if (images.isNotEmpty()) flush()
            }
            is CompositeHexoParser.TextContext -> buffer.append(node.text)
            else -> visitChildren(node)
        }
    }

    private suspend fun visitChildren(node: ParserRuleContext) {
        node.children.orEmpty().filterIsInstance<ParserRuleContext>().forEach { visit(it) }
    }

    private suspend fun renderBlock(raw: String) {
        val image = image(raw.blockNotation(), isParagraph = true)
        if (image == null) {
            buffer.append(raw)
        } else {
            flush()
            gallery(listOf(image))
        }
    }

    private suspend fun renderInline(raw: String, notation: String?) {
        buffer.append(raw)
        image(notation, isParagraph = false)?.let { images += it }
    }

    private suspend fun renderSpoiler(node: CompositeHexoParser.SpoilerContext) {
        buffer.spoiler(open = true)
        visitChildren(node)
        if (node.SPOILER().size == 2) buffer.spoiler(open = false)
    }

    private suspend fun image(notation: String?, isParagraph: Boolean): MediaGalleryItem? =
        notation?.let { renderImage(it, isParagraph) }?.withSpoiler(buffer.isSpoiler)

    private fun gallery(items: List<MediaGalleryItem>) {
        for (chunk in items.chunked(10)) {
            if (lastWasGallery) scope.separator()
            scope.mediaGallery(items = chunk.toTypedArray())
            lastWasGallery = true
        }
    }

    private fun flush(endOfMessage: Boolean = false) {
        val value = buffer.take(closeSpoiler = !endOfMessage)
        if (value.isNotBlank()) {
            scope.text(value)
            lastWasGallery = false
        }
        gallery(images)
        images.clear()
    }
}

private class NotationTextBuffer {
    private val content = StringBuilder()
    private var startsInSpoiler = false
    private var endsWithDelimiter = false
    var isSpoiler = false
        private set

    fun append(value: String) {
        content.append(value)
        endsWithDelimiter = false
    }

    fun spoiler(open: Boolean) {
        content.append("||")
        isSpoiler = open
        endsWithDelimiter = true
    }

    fun take(closeSpoiler: Boolean): String {
        var value = content.toString()
        if (value.isNotEmpty() && startsInSpoiler) {
            value = if (value.startsWith("||")) value.drop(2) else "||$value"
        }
        if (value.isNotEmpty() && isSpoiler && closeSpoiler) {
            value = if (endsWithDelimiter) value.dropLast(2) else "$value||"
        }
        content.clear()
        startsInSpoiler = isSpoiler
        endsWithDelimiter = false
        return value
    }
}

private fun String.inlineNotation(): String? =
    takeIf { length >= 2 && endsWith('`') }?.removeSurrounding("`")

private fun String.blockNotation(): String? {
    if (length < 6 || !endsWith("```")) return null
    val body = removeSurrounding("```")
    if ('\n' !in body) return body
    val language = body.substringBefore('\n').trim()
    return body.substringAfter('\n').takeIf { language.isEmpty() || language == "hexo" }
}

private fun String.notationUrl(): String {
    var url = this
    do {
        val previous = url
        url = url.trimEnd('.', ',', '!', '?', ';', ':')
        if (url.endsWith(')') && url.count { it == ')' } > url.count { it == '(' }) {
            url = url.dropLast(1)
        }
    } while (url != previous)
    return url
}

data class ImageSizeLimits(val required: String, val limit: String)

@LocalizationBundle("render")
interface RenderLocalization {
    fun missingImage(): String
    fun imageTooLarge(): ErrorResponseMessage<ImageSizeLimits>
}
