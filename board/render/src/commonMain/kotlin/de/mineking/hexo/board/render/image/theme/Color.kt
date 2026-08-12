package de.mineking.hexo.board.render.image.theme

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@JvmInline
@Serializable
value class Color private constructor(val rgba: Int) {
    val alpha get() = (rgba shr 3 * 8 and 0xff)
    val red get() = (rgba shr 2 * 8 and 0xff)
    val green get() = (rgba shr 1 * 8 and 0xff)
    val blue get() = (rgba shr 0 * 8 and 0xff)

    companion object {
        val Transparent = rgba(0)
        private fun format(len: Int) = HexFormat {
            number {
                minLength = len
                removeLeadingZeros = true
            }
        }

        fun rgb(rgb: Int) = Color(rgb or 0xff000000.toInt())
        fun rgba(rgba: Long) = Color(rgba.toInt())

        fun of(red: Int, green: Int, blue: Int, alpha: Int) = Color(
            rgba = (alpha shl (3 * 8)) +
                (red shl (2 * 8)) +
                (green shl (1 * 8)) +
                (blue shl (0 * 8)),
        )

        fun parse(hex: String): Color {
            val h = hex.removePrefix("#")

            fun nibble(c: Char) = c.digitToInt(16)
            fun byte(s: String) = s.toInt(16)

            val (r, g, b, a) = when (h.length) {
                3 -> intArrayOf(
                    nibble(h[0]) * 17,
                    nibble(h[1]) * 17,
                    nibble(h[2]) * 17,
                    255,
                )

                4 -> intArrayOf(
                    nibble(h[0]) * 17,
                    nibble(h[1]) * 17,
                    nibble(h[2]) * 17,
                    nibble(h[3]) * 17,
                )

                6 -> intArrayOf(
                    byte(h.substring(0, 2)),
                    byte(h.substring(2, 4)),
                    byte(h.substring(4, 6)),
                    255,
                )

                8 -> intArrayOf(
                    byte(h.substring(0, 2)),
                    byte(h.substring(2, 4)),
                    byte(h.substring(4, 6)),
                    byte(h.substring(6, 8)),
                )

                else -> throw IllegalArgumentException("Invalid color: $hex")
            }

            return of(r, g, b, a)
        }
    }

    override fun toString() = "#${(rgba and 0xffffff).toHexString(format(6))}${alpha.toHexString(format(2))}"
}

fun Color.isTransparent() = alpha == 0

fun Color.withAlpha(alpha: Int, force: Boolean = false) =
    if (!force && alpha >= this.alpha) {
        this
    } else {
        Color.rgba((((alpha and 0xff) shl (3 * 8)) or (rgba and 0xffffff)).toLong())
    }

private fun Int.tint(target: Int, factor: Double) = (toDouble() + (target.toDouble() - toDouble()) * factor)
    .coerceIn(0.0, 255.0)
    .toInt()

fun Color.tint(target: Color): Color {
    val factor = target.alpha.toDouble() / 255.0

    return Color.of(
        red.tint(target.red, factor),
        green.tint(target.green, factor),
        blue.tint(target.blue, factor),
        alpha,
    )
}

fun Color.isDark() = red * 299 + green * 587 + blue * 114 < 128_000

private const val DEFAULT_BLEND_FACTOR = 0.65

private data class Hsl(val hue: Double, val saturation: Double, val lightness: Double)

private fun Color.toHsl(): Hsl {
    val r = red / 255.0
    val g = green / 255.0
    val b = blue / 255.0
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val lightness = (max + min) / 2.0

    if (max == min) return Hsl(0.0, 0.0, lightness)

    val delta = max - min
    val saturation = delta / (1.0 - kotlin.math.abs(2.0 * lightness - 1.0))
    val hue = when (max) {
        r -> ((g - b) / delta).mod(6.0)
        g -> (b - r) / delta + 2.0
        else -> (r - g) / delta + 4.0
    } / 6.0

    return Hsl(hue, saturation, lightness)
}

private fun Hsl.toColor(alpha: Int): Color {
    fun hueToRgb(p: Double, q: Double, hue: Double): Double {
        val h = hue.mod(1.0)
        return when {
            h < 1.0 / 6.0 -> p + (q - p) * 6.0 * h
            h < 1.0 / 2.0 -> q
            h < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - h) * 6.0
            else -> p
        }
    }

    val q = if (lightness < 0.5) {
        lightness * (1.0 + saturation)
    } else {
        lightness + saturation - lightness * saturation
    }
    val p = 2.0 * lightness - q
    fun Double.channel() = (coerceIn(0.0, 1.0) * 255.0).roundToInt()

    return Color.of(
        hueToRgb(p, q, hue + 1.0 / 3.0).channel(),
        hueToRgb(p, q, hue).channel(),
        hueToRgb(p, q, hue - 1.0 / 3.0).channel(),
        alpha,
    )
}

private fun Color.adjustLightness(target: Double, factor: Double): Color {
    val hsl = toHsl()
    val lightness = hsl.lightness + (target - hsl.lightness) * factor.coerceIn(0.0, 1.0)
    return hsl.copy(lightness = lightness).toColor(alpha)
}

fun Color.darker(factor: Double = DEFAULT_BLEND_FACTOR) = adjustLightness(0.0, factor)
fun Color.brighter(factor: Double = DEFAULT_BLEND_FACTOR) = adjustLightness(1.0, factor)
