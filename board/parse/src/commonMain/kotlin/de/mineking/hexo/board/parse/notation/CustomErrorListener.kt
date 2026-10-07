package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.requireHexo
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.LexerNoViableAltException
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.TokenStream
import org.antlr.v4.kotlinruntime.Vocabulary
import org.antlr.v4.kotlinruntime.misc.IntervalSet
import org.antlr.v4.kotlinruntime.tree.ErrorNode
import org.antlr.v4.kotlinruntime.tree.ParseTreeListener
import org.antlr.v4.kotlinruntime.tree.TerminalNode

internal fun <P : Parser, R> parseANTLRNotation(
    input: String,
    createLexer: (CharStream) -> Lexer,
    createParser: (TokenStream) -> P,
    parse: (P) -> R,
): R {
    val listener = CustomErrorListener(input)
    val lexer = createLexer(CharStreams.fromString(input)).apply {
        removeErrorListeners()
        addErrorListener(listener)
    }
    val parser = createParser(CommonTokenStream(lexer)).apply {
        removeErrorListeners()
        addErrorListener(listener)
        addParseListener(listener)
    }

    val result = parse(parser)
    listener.throwIfInvalid()
    return result
}

internal fun IntervalSet.formatExpectedTokenNames(vocabulary: Vocabulary): String {
    val displayTokens = IntervalSet(this)
    for (tokenType in toList()) {
        if (vocabulary.getSymbolicName(tokenType) == "WS") displayTokens.remove(tokenType)
    }
    if (displayTokens.isNil && !isNil) return "whitespace"

    val displayVocabulary = object : Vocabulary by vocabulary {
        override fun getDisplayName(tokenType: Int) = vocabulary.getLiteralName(tokenType)
            ?: vocabulary.getDisplayName(tokenType).lowercase()
    }
    return displayTokens.toString(displayVocabulary).replace("<EOF>", "<eof>")
}

internal class CustomErrorListener(private val input: String) : BaseErrorListener(), ParseTreeListener {
    private data class SyntaxError(
        val offset: Int,
        val message: String,
        val insideRule: Boolean,
        val expectsOnlyEOF: Boolean,
    )

    private var firstError: SyntaxError? = null
    private val matchedTokens = mutableListOf<Token>()

    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?,
    ) {
        val offset = (offendingSymbol as? Token)?.startIndex
            ?: (e as? LexerNoViableAltException)?.startIndex
            ?: input.length
        if (firstError == null || offset < firstError!!.offset) {
            val parser = recognizer as? Parser
            val expected = parser?.expectedTokens
            firstError = SyntaxError(
                offset,
                "Invalid notation at $line:${charPositionInLine + 1}: ${formatMessage(msg, parser, offendingSymbol as? Token)}",
                parser?.context?.getParent() != null,
                expected != null && expected.size() == 1 && Token.EOF in expected,
            )
        }
    }

    private fun formatMessage(message: String, parser: Parser?, offendingToken: Token?): String {
        if (parser == null) return message

        val vocabulary = parser.vocabulary
        val expected = parser.expectedTokens
        val originalNames = expected.toString(vocabulary)
        val displayNames = expected.formatExpectedTokenNames(vocabulary)

        // Only rewrite expected token names; quoted literals and offending input keep their case.
        var result = when {
            message.endsWith(" expecting $originalNames") -> message.removeSuffix(originalNames) + displayNames
            message.startsWith("missing $originalNames at ") -> message.replaceFirst("missing $originalNames at ", "missing $displayNames at ")
            else -> message
        }
        if (offendingToken?.type == Token.EOF) {
            result = result.replace("'<EOF>'", "'<eof>'")
        }
        return result
    }

    override fun visitTerminal(node: TerminalNode) {
        val token = node.symbol
        if (token.type != Token.EOF && token.tokenIndex >= 0 && !token.text.isNullOrBlank()) {
            matchedTokens += token
        }
    }

    override fun visitErrorNode(node: ErrorNode) = Unit
    override fun enterEveryRule(ctx: ParserRuleContext) = Unit
    override fun exitEveryRule(ctx: ParserRuleContext) = Unit

    fun throwIfInvalid() {
        val error = firstError ?: return
        // Lookahead may report a lexer error before visitTerminal receives the preceding match.
        // Only real matches before the earliest error count; recovery cannot establish a prefix.
        val prefix = matchedTokens.filter { it.stopIndex < error.offset }
        val matchedCharacters = prefix.sumOf { token -> token.text.orEmpty().count { !it.isWhitespace() } }
        val inputCharacters = input.count { !it.isWhitespace() }

        val matchedLiteral = prefix.any { token ->
            val literal = (token.tokenSource as? Lexer)?.vocabulary?.getLiteralName(token.type)
            literal != null && literal.length > 3 // Exclude the quotes and single-character literals.
        }

        // A single shared token is weak evidence unless it covers most of the input or the
        // parser has already entered a more specific rule. Longer literals and multiple
        // matches establish a prefix without depending on the notation's vocabulary.
        val recognized = prefix.isNotEmpty() &&
            (error.insideRule || error.expectsOnlyEOF || matchedLiteral || prefix.size >= 2 ||
                matchedCharacters >= (inputCharacters + 1) / 2)
        requireHexo(false, notationCheck = !recognized) { error.message }
    }
}
