package de.mineking.hexo.board.latex

import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.LatexCommand
import de.mineking.kotlinlatex.latex
import de.mineking.kotlinlatex.raw

@LatexCommand("expandhexomacros")
fun expandHexoMacros(
    macros: Latex = latex(""),
    notation: Latex,
) = raw(
    notation.source
        .expandMacros(macros.source.parseMacroDefinitions())
        .expandCrossCellLabels(),
)

private fun String.expandCrossCellLabels() = buildString {
    var labelDepth = 0
    var expanding = false
    val macro = StringBuilder()

    this@expandCrossCellLabels.forEach { char ->
        when {
            expanding && char == '}' -> {
                macro.toString().forEach { append(".[$it]") }
                macro.clear()
                expanding = false
            }
            expanding -> macro.append(char)
            char == '[' -> {
                labelDepth++
                append(char)
            }
            char == ']' -> {
                labelDepth--
                append(char)
            }
            char == '{' && labelDepth == 0 -> expanding = true
            else -> append(char)
        }
    }

    if (expanding) append('{').append(macro.toString())
}

private fun String.parseMacroDefinitions(): List<Pair<String, String>> {
    val replacementSource = this
        .removePrefix("{")
        .removeSuffix("}")

    if (replacementSource.isBlank()) return emptyList()

    return replacementSource
        .split(',')
        .filter { it.isNotBlank() }
        .map { entry ->
            val separator = entry.indexOf('=')
            require(separator > 0) { "Invalid replacement `$entry`; expected input=output" }

            val input = entry.substring(0, separator).trim()
            val output = entry.substring(separator + 1).trim()
            require(input.isNotEmpty()) { "Replacement input must not be empty" }
            input to output
        }
}

private fun String.expandMacros(macros: List<Pair<String, String>>) = buildString {
    var index = 0
    while (index < this@expandMacros.length) {
        var match: Pair<String, String>? = null

        macros.forEach { macro ->
            if (
                this@expandMacros.startsWith(macro.first, index) &&
                macro.first.length > (match?.first?.length ?: -1)
            ) {
                match = macro
            }
        }

        val selected = match
        if (selected == null) {
            append(this@expandMacros[index])
            index++
        } else {
            append(selected.second)
            index += selected.first.length
        }
    }
}
