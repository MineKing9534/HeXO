package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.HexoNotationFormatException
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer

internal class CustomErrorListener : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?,
    ) {
        throw HexoNotationFormatException(
            "Invalid notation at $line:${charPositionInLine + 1}: $msg",
        )
    }
}
