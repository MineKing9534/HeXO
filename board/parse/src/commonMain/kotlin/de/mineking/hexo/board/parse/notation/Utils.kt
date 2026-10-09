package de.mineking.hexo.board.parse.notation

import de.mineking.hexo.board.requireHexo

internal fun String.unescape() = buildString {
    var index = 0
    while (index <= this@unescape.lastIndex) {
        if (this@unescape[index] == '\\') index++
        requireHexo(index <= this@unescape.lastIndex) { "Trailing `\\`" }

        append(this@unescape[index])
        index++
    }
}
