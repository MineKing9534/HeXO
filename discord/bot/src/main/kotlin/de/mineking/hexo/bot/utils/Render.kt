package de.mineking.hexo.bot.utils

import de.mineking.discord.localization.Locale
import de.mineking.discord.localization.LocalizationFile
import de.mineking.discord.localization.Localize
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.hexo.bot.HeXODiscordBot
import de.mineking.hexo.bot.localization
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

context(main: HeXODiscordBot)
suspend fun Board.asMediaGalleryItem(theme: Theme) = main.boardRenderer.render(this, theme).toMediaGalleryItem()

context(main: HeXODiscordBot)
suspend fun IReplyCallback.replyRichHexoNotation(content: String, theme: Theme) {
    if (!isAcknowledged) deferReply().queue()

    val components = content.renderToComponents(theme).layout()
    if (components.filterIsInstance<MediaGallery>().isEmpty()) {
        val errorMessage = renderAsComponent(MessageColor.Error, main.localization<RenderLocalization>().responseError(userLocale))
        hook.editOriginalComponents(errorMessage).useComponentsV2().queue()
    } else {
        hook.editOriginalComponents(components).useComponentsV2().queue()
    }
}

interface RenderLocalization : LocalizationFile {
    @Localize
    fun responseError(@Locale locale: DiscordLocale): String
}

private fun List<MessageTopLevelComponent>.layout(): List<MessageTopLevelComponent> = fold(mutableListOf()) { result, current ->
    val last = result.lastOrNull()
    when (current) {
        is TextDisplay if last is TextDisplay -> {
            result[result.lastIndex] = TextDisplay.of(last.content + current.content)
        }
        is MediaGallery if last is MediaGallery -> {
            result += Separator.createDivider(Separator.Spacing.LARGE)
            result += current
        }
        else -> result += current
    }

    result
}

private class ComponentParserState {
    val result = mutableListOf<MessageTopLevelComponent>()
    val afterParagraph = mutableListOf<MediaGalleryItem>()
    val temp = StringBuilder()
    var position = 0

    fun flush() {
        if (temp.isNotBlank()) {
            result += TextDisplay.of(temp.toString())
            temp.clear()
        }

        if (afterParagraph.isNotEmpty()) {
            result += MediaGallery.of(afterParagraph)
            afterParagraph.clear()
        }
    }
}

context(main: HeXODiscordBot)
private suspend fun String.renderToComponents(theme: Theme) = try {
    val board = main.notationParser.parse(this)
    listOf(MediaGallery.of(board.asMediaGalleryItem(theme)))
} catch (_: HexoNotationException) {
    val state = context(theme) { internalRender() }
    state.flush()

    state.result
}

context(main: HeXODiscordBot, theme: Theme)
private suspend fun String.internalRender(): ComponentParserState {
    val state = ComponentParserState()

    while (state.position < length) {
        val segment = SegmentParser.entries.firstNotNullOfOrNull { parser ->
            parser.match(this, state.position)?.let { parser to it }
        }

        state.handle(this, segment)
    }

    return state
}

context(main: HeXODiscordBot, theme: Theme)
private suspend fun ComponentParserState.handle(str: String, segment: Pair<SegmentParser, SegmentMatch>?) {
    if (segment == null) {
        val c = str[position]
        temp.append(c)

        if (c == '\n') flush()
        position++
    } else {
        val (type, match) = segment

        if (type.keepAsText) {
            temp.append(match.text)
        } else {
            flush()
        }

        type.handle(match.content, this)
        position = match.end
    }
}

private data class SegmentMatch(
    val content: String,
    val text: String,
    val end: Int,
)

private enum class SegmentParser(val symbol: String?, val keepAsText: Boolean) {
    CodeBlock("```", keepAsText = false) {
        context(main: HeXODiscordBot, theme: Theme)
        override suspend fun handle(content: String, state: ComponentParserState) {
            val (code, lang) = content.decodeCodeAndLanguage()
            state.result += try {
                val notation = if (lang == "hexo" || lang == null) code else "$lang\n$code"
                MediaGallery.of(main.notationParser.parse(notation).asMediaGalleryItem(theme))
            } catch (_: HexoNotationException) {
                TextDisplay.of("$symbol${lang?.let { "$it\n" } ?: ""}$code$symbol")
            }
        }

        private fun String.decodeCodeAndLanguage(): Pair<String, String?> {
            val newline = indexOf('\n')
            return if (newline == -1) {
                this to null
            } else {
                val code = substring(newline + 1)
                val lang = substring(0, newline).ifBlank { null }
                code to lang
            }
        }
    },
    Code("`", keepAsText = true) {
        context(main: HeXODiscordBot, theme: Theme)
        override suspend fun handle(content: String, state: ComponentParserState) {
            try {
                state.afterParagraph += main.notationParser.parse(content).asMediaGalleryItem(theme)
            } catch (_: HexoNotationException) {
            }
        }
    },
    Url(symbol = null, keepAsText = true) {
        private val pattern = """https?://\S+""".toRegex()
        private val trailingPunctuation = charArrayOf('.', ',', ';', ':', '!', '?')

        override fun match(str: String, position: Int): SegmentMatch? {
            val value = pattern.find(str, position)?.takeIf { it.range.first == position }?.value ?: return null
            val content = value.trimEnd { it in trailingPunctuation }

            return SegmentMatch(
                content = content,
                text = content,
                end = position + content.length,
            )
        }

        context(main: HeXODiscordBot, theme: Theme)
        override suspend fun handle(content: String, state: ComponentParserState) {
            try {
                state.afterParagraph += main.notationParser.parse(content).asMediaGalleryItem(theme)
            } catch (_: HexoNotationException) {
            }
        }
    },
    Spoiler("||", keepAsText = false) {
        context(main: HeXODiscordBot, theme: Theme)
        override suspend fun handle(content: String, state: ComponentParserState) {
            val innerState = content.internalRender()
            if (innerState.temp.isNotEmpty()) {
                state.temp.append("$symbol${innerState.temp}$symbol")
            }

            state.afterParagraph += innerState.afterParagraph.map { it.withSpoiler(true) }
            state.result += innerState.result.map {
                when (it) {
                    is MediaGallery -> it.withItems(it.items.map { item -> item.withSpoiler(true) })
                    else -> it
                }
            }
        }
    },
    ;

    open fun match(str: String, position: Int): SegmentMatch? {
        val symbol = symbol ?: return null
        if (!str.startsWith(symbol, position)) return null

        val start = position + symbol.length
        val end = str.indexOf(symbol, start)
        if (end == -1) return null

        val content = str.substring(start, end)
        return SegmentMatch(
            content = content,
            text = "$symbol$content$symbol",
            end = end + symbol.length,
        )
    }

    context(main: HeXODiscordBot, theme: Theme)
    abstract suspend fun handle(content: String, state: ComponentParserState)
}
