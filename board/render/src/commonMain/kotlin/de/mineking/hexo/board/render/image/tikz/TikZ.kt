package de.mineking.hexo.board.render.image.tikz

/**
 * A deliberately small DSL for the subset of TikZ used by the board renderer.
 *
 * Coordinates are expressed in the surrounding picture's units. Options are
 * rendered in insertion order, which also makes generated output deterministic.
 */
@DslMarker
annotation class TikZDsl

data class TikZPoint(val x: Double, val y: Double) {
    override fun toString() = "(${x.tikzNumber()}, ${y.tikzNumber()})"
}

sealed interface TikZPathSegment {
    data class Line(val point: TikZPoint) : TikZPathSegment
    data class QuadraticCurve(val control: TikZPoint, val point: TikZPoint) : TikZPathSegment
}

data class TikZPath(
    val start: TikZPoint,
    val segments: List<TikZPathSegment> = emptyList(),
    val closed: Boolean = false,
)

@TikZDsl
class TikZPicture internal constructor(
    private val commands: MutableList<String>,
) {
    fun raw(command: String) {
        commands += command
    }

    fun path(path: TikZPath, options: List<String>) {
        val value = buildString {
            append("\\path")
            appendOptions(options)
            append(' ')
            append(path.start)
            path.segments.forEach {
                when (it) {
                    is TikZPathSegment.Line -> append(" -- ").append(it.point)
                    is TikZPathSegment.QuadraticCurve -> append(" .. controls ")
                        .append(it.control)
                        .append(" .. ")
                        .append(it.point)
                }
            }
            if (path.closed) append(" -- cycle")
            append(';')
        }
        commands += value
    }

    fun line(from: TikZPoint, to: TikZPoint, options: List<String>) =
        path(TikZPath(from, listOf(TikZPathSegment.Line(to))), options)

    fun clip(path: TikZPath) {
        val value = buildString {
            append("\\clip ")
            append(path.start)
            path.segments.forEach {
                when (it) {
                    is TikZPathSegment.Line -> append(" -- ").append(it.point)
                    is TikZPathSegment.QuadraticCurve -> append(" .. controls ")
                        .append(it.control)
                        .append(" .. ")
                        .append(it.point)
                }
            }
            if (path.closed) append(" -- cycle")
            append(';')
        }
        commands += value
    }

    fun circle(center: TikZPoint, radius: Double, options: List<String>) {
        commands += buildString {
            append("\\path")
            appendOptions(options)
            append(' ')
            append(center)
            append(" circle[radius=")
            append(radius.tikzNumber())
            append("bp];")
        }
    }

    fun node(point: TikZPoint, text: String, options: List<String>) {
        commands += buildString {
            append("\\node")
            appendOptions(options)
            append(" at ")
            append(point)
            append(" {")
            append(escapeTikZ(text))
            append("};")
        }
    }

    fun scope(block: TikZPicture.() -> Unit) {
        commands += "\\begin{scope}"
        block()
        commands += "\\end{scope}"
    }
}

fun tikzPicture(
    options: List<String> = emptyList(),
    width: String? = "\\textwidth",
    block: TikZPicture.() -> Unit,
): String {
    val commands = mutableListOf<String>()
    TikZPicture(commands).block()
    return buildString {
        if (width != null) {
            append("\\resizebox{")
            append(width)
            append("}{!}{%\n")
        }
        append("\\begin{tikzpicture}")
        appendOptions(options)
        append('\n')
        commands.forEach { append("  ").append(it).append('\n') }
        append("\\end{tikzpicture}")
        if (width != null) append("%\n}")
    }
}

private fun StringBuilder.appendOptions(options: List<String>) {
    if (options.isNotEmpty()) append(options.joinToString(prefix = "[", postfix = "]"))
}

fun Double.tikzNumber(): String {
    require(isFinite()) { "TikZ coordinates must be finite" }
    val value = toString()
    return if (value == "-0.0") "0" else value.removeSuffix(".0")
}

fun escapeTikZ(value: String) = buildString {
    value.forEach {
        append(
            when (it) {
                '\\' -> "\\textbackslash{}"
                '{' -> "\\{"
                '}' -> "\\}"
                '$' -> "\\$"
                '&' -> "\\&"
                '#' -> "\\#"
                '_' -> "\\_"
                '%' -> "\\%"
                '^' -> "\\textasciicircum{}"
                '~' -> "\\textasciitilde{}"
                else -> it
            },
        )
    }
}
