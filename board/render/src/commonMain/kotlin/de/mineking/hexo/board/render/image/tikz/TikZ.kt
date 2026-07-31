package de.mineking.hexo.board.render.image.tikz

@DslMarker
annotation class TikZDsl

data class TikZPoint(val x: Double, val y: Double) {
    override fun toString() = "(${x.tikzNumber()}, ${y.tikzNumber()})"
}

sealed interface TikZPathSegment {
    data class Line(val point: TikZPoint) : TikZPathSegment
    data class CubicCurve(val control1: TikZPoint, val control2: TikZPoint, val point: TikZPoint) : TikZPathSegment
}

data class TikZPath(
    val start: TikZPoint,
    val segments: List<TikZPathSegment> = emptyList(),
    val closed: Boolean = false,
)

@TikZDsl
class TikZPicture internal constructor() {
    private val commands = mutableListOf<String>()
    private val beforeCommands = mutableListOf<String>()

    fun declareFading(name: String, options: List<String> = emptyList(), block: TikZPicture.() -> Unit) {
        val fading = TikZPicture()
        fading.block()
        // The `%` comments out the line breaks around the nested `tikzpicture`. Without them the
        // line breaks would turn into spaces inside the fading's box, which would widen the box
        // asymmetrically and shift the whole fading off center.
        beforeCommands += "\\pgfdeclarefading{$name}{%\n" + fading.render(options) + "%\n}"
    }

    fun path(path: TikZPath, options: List<String> = emptyList()) = addPathCommand("\\path", path, options)
    fun line(from: TikZPoint, to: TikZPoint, options: List<String>) = path(TikZPath(from, listOf(TikZPathSegment.Line(to))), options)
    fun clip(path: TikZPath) = addPathCommand("\\clip", path)

    fun circle(center: TikZPoint, radius: Double, options: List<String>) {
        addCommand {
            append("\\path")
            appendOptions(options)
            append(' ')
            append(center)
            append(" circle[radius=")
            append(radius.tikzNumber())
            append("bp];")
        }
    }

    fun node(point: TikZPoint, text: String, options: List<String> = emptyList()) {
        nodeRaw(point, escapeTikZ(text), options)
    }

    fun nodeRaw(point: TikZPoint, content: String, options: List<String> = emptyList()) {
        addCommand {
            append("\\node")
            appendOptions(options)
            append(" at ")
            append(point)
            append(" {")
            append(content)
            append("};")
        }
    }

    fun scope(block: TikZPicture.() -> Unit) {
        commands += "\\begin{scope}"
        block()
        commands += "\\end{scope}"
    }

    internal fun render(options: List<String>) = buildString {
        beforeCommands.forEach { append(it).append('\n') }
        append("\\begin{tikzpicture}")
        appendOptions(options)
        append('\n')
        commands.forEach { append("  ").append(it).append('\n') }
        append("\\end{tikzpicture}")
    }

    private fun addPathCommand(
        command: String,
        path: TikZPath,
        options: List<String> = emptyList(),
    ) {
        addCommand {
            append(command)
            appendOptions(options)
            append(' ')
            appendPath(path)
            append(';')
        }
    }

    private fun addCommand(block: StringBuilder.() -> Unit) {
        commands += buildString(block)
    }
}

fun tikzPicture(
    options: List<String> = emptyList(),
    block: TikZPicture.() -> Unit,
): String {
    val picture = TikZPicture()
    picture.block()
    return picture.render(options)
}

private fun StringBuilder.appendPath(path: TikZPath) {
    append(path.start)
    path.segments.forEach { segment ->
        when (segment) {
            is TikZPathSegment.Line -> append(" -- ").append(segment.point)
            is TikZPathSegment.CubicCurve -> append(" .. controls ")
                .append(segment.control1)
                .append(" and ")
                .append(segment.control2)
                .append(" .. ")
                .append(segment.point)
        }
    }

    if (path.closed) append(" -- cycle")
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
