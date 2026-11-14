package de.mineking.hexo.discord.bot.utils

import de.mineking.discord.markdown.markdown
import de.mineking.hexo.board.Board
import de.mineking.hexo.board.render.BoardRenderer
import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.HDSTheme
import de.mineking.hexo.board.render.notation.CombinedNotationBoardRenderer
import de.mineking.hexo.board.render.notation.HTTTXNotationBoardRenderer
import de.mineking.hexo.board.render.notation.RectilinearNotationBoardRenderer
import de.mineking.hexo.board.render.notation.RectilinearNotationType
import de.mineking.hexo.board.render.notation.TytoNotationBoardRenderer
import de.mineking.hexo.board.render.render

enum class NotationType(val renderer: BoardRenderer<Boolean, String>) {
    CompactRectilinear(RectilinearNotationBoardRenderer.withType(RectilinearNotationType.Compact).highlight()),
    MultilineRectilinear(RectilinearNotationBoardRenderer.withType(RectilinearNotationType.Multiline).highlight()),
    Combined(CombinedNotationBoardRenderer.codeBlock()),
    HTTTX(HTTTXNotationBoardRenderer.codeBlock()),
    Tyto(TytoNotationBoardRenderer.prefix("https://hexo.tyto.cc/analysis#c=")),
}

private fun BoardRenderer<Unit, String>.prefix(prefix: String) = transform { value, _ -> prefix + value }
private fun BoardRenderer<Unit, String>.highlight() = codeBlock("ansi") {
    when (it) {
        'x', 'X' -> it.color(HDSTheme.Default.playerXColor)
        'o', 'O' -> it.color(HDSTheme.Default.playerOColor)
        '!' -> it.color(HDSTheme.Default.highlightColor)
        else -> "$it"
    }
}

private fun BoardRenderer<Unit, String>.codeBlock(lang: String = "hexo", transform: (Char) -> String = { "$it" }) = transform { value, styled ->
    markdown {
        if (styled) {
            codeBlock(value.map { transform(it) }.joinToString(""), language = lang)
        } else {
            codeBlock(value, "hexo")
        }
    }
}

private fun BoardRenderer<Unit, String>.transform(transform: (String, Boolean) -> String) = object : BoardRenderer<Boolean, String> {
    override suspend fun render(board: Board, param: Boolean) = transform(this@transform.render(board), param)
}

private fun Char.color(color: Color) = "\u001B[1;38;2;${color.red};${color.green};${color.blue}m$this\u001B[0m"
