package de.mineking.hexo.board.latex

import de.mineking.hexo.board.render.image.theme.Color
import de.mineking.hexo.board.render.image.theme.Theme
import de.mineking.kotlinlatex.Latex
import de.mineking.kotlinlatex.raw

@RequiresOptIn
annotation class InternalHexoLatexApi

@InternalHexoLatexApi
fun themeSpec(name: String, values: List<Latex>) = raw(values.joinToString(prefix = "$name|", separator = "|") { it.source })

@InternalHexoLatexApi
fun String.orDefault(default: Double) = if (isEmpty()) default else toDouble()

@InternalHexoLatexApi
fun String.orDefault(default: Color) = if (isEmpty()) default else Color.parse(this)

interface ThemeFactory {
    fun createTheme(args: List<String>): Theme
}

@InternalHexoLatexApi
object HexoThemeRegistry {
    private val themes = mutableMapOf<String, ThemeFactory>()

    fun registerTheme(name: String, factory: ThemeFactory) {
        require(name !in themes) { "Theme '$name' already defined" }
        themes[name] = factory
    }

    fun parseTheme(spec: String): Theme {
        val args = spec.split("|")
        val name = args[0]

        val factory = themes[name] ?: error("Unknown theme '$name'")
        return factory.createTheme(args.drop(1))
    }
}

private val themeCache = mutableMapOf<String, Theme>()

@OptIn(InternalHexoLatexApi::class)
fun Latex.parseTheme(): Theme {
    val specification = raw(source.removeSurrounding("{", "}"))
    return themeCache.getOrPut(specification.source) { HexoThemeRegistry.parseTheme(specification.source) }
}
