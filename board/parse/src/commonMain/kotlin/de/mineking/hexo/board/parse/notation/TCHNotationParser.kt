package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.Direction
import de.mineking.hexo.board.HexoNotationException
import de.mineking.hexo.board.merge
import de.mineking.hexo.board.mirror
import de.mineking.hexo.board.parse.NotationParser
import de.mineking.hexo.board.parse.generated.TCHNLexer
import de.mineking.hexo.board.parse.generated.TCHNParser
import de.mineking.hexo.board.requireHexo
import de.mineking.hexo.board.rotate

object TCHNotationParser : NotationParser {
    override suspend fun parse(notation: String) = notation.parseTCHNotation()
}

suspend fun String.parseTCHNotation(parser: NotationParser = NotationParser.Default): Board {
    val root = parseInternal(this)
    return root.boardExpression().parse(parser)
}

private suspend fun TCHNParser.BoardExpressionContext.parse(parser: NotationParser): Board {
    val notation = NOTATION()
    if (notation != null) {
        try {
            return parser.parse(notation.text.removeSurrounding("{", "}"))
        } catch (e: HexoNotationException) {
            throw HexoNotationException("Invalid expression notation: ${e.message}", e)
        }
    }

    val function = checkNotNull(functionCall())
    val identifier = function.IDENTIFIER().text
    val arguments = FunctionArguments(identifier, function.argument(), parser)

    return when (identifier) {
        "rotate" -> {
            val sectors = arguments.nextInt()
            val board = arguments.nextBoard()
            arguments.checkFinished()

            board.rotate(sectors)
        }
        "mirror" -> {
            val axis = arguments.nextAxis()
            val board = arguments.nextBoard()
            arguments.checkFinished()

            board.mirror(axis)
        }
        "merge" -> {
            requireHexo(arguments.size >= 2) { "Need at least 2 boards to merge" }
            merge(arguments.remaining().map { it.asBoard() })
        }
        else -> throw HexoNotationException("Unknown function $function")
    }
}

private class FunctionArguments(
    private val function: String,
    private val arguments: List<TCHNParser.ArgumentContext>,
    private val parser: NotationParser,
) {
    private var current = 0

    val size = arguments.size
    private fun hasNext() = current < arguments.size

    fun checkFinished() {
        requireHexo(!hasNext()) { "Unexpected trailing arguments for function '$function'" }
    }

    private fun next(): FunctionArgument {
        requireHexo(hasNext()) { "Missing argument for function '$function'" }
        val arg = arguments[current++]

        return FunctionArgument(
            function = function,
            position = current,
            arg = arg,
            parser = parser,
        )
    }

    suspend fun nextBoard() = next().asBoard()
    fun nextInt() = next().asInt()
    fun nextAxis() = next().asAxis()

    fun remaining() = arguments.subList(current, arguments.size)
        .mapIndexed { index, arg -> FunctionArgument(function, current + index + 1, arg, parser) }
        .also { current = arguments.size }
}

private class FunctionArgument(
    private val function: String,
    private val position: Int,
    private val arg: TCHNParser.ArgumentContext,
    private val parser: NotationParser,
) {
    private fun <T> requireArgument(type: String, value: T?): T {
        requireHexo(value != null) { "Expected $type at $position for '$function'" }
        return value
    }

    suspend fun asBoard() = requireArgument("board", arg.boardExpression()).parse(parser)

    fun asInt(): Int {
        val text = requireArgument("integer", arg.INTEGER()).text
        return text.toIntOrNull()
            ?: throw HexoNotationException("Integer `$text` at $position for '$function' is out of range")
    }

    fun asAxis() = when (requireArgument("axis", arg.AXIS()).text) {
        "-" -> Direction.Right
        "/" -> Direction.TopRight
        "\\" -> Direction.TopLeft
        else -> throw HexoNotationException("Invalid axis")
    }
}

private fun parseInternal(input: String) = parseANTLRNotation(input, ::TCHNLexer, ::TCHNParser, TCHNParser::root)
