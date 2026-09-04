package de.mineking.hexo.hds.implementation.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ColorSerializer::class)
internal data class Color(val red: Int, val green: Int, val blue: Int) {
    init {
        require(red in 0..255 && green in 0..255 && blue in 0..255)
    }

    private val Int.hex get() = toString(16).uppercase().padStart(2, '0')
    fun format() = "#${red.hex}${green.hex}${blue.hex}"

    companion object {
        fun decode(input: String): Color {
            val s = input.removePrefix("#")
            require(s.length == 6)

            return Color(
                red = s.substring(0, 2).toInt(16),
                green = s.substring(2, 4).toInt(16),
                blue = s.substring(4, 6).toInt(16),
            )
        }
    }
}

internal object ColorSerializer : KSerializer<Color> {
    override val descriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) = encoder.encodeString(value.format())
    override fun deserialize(decoder: Decoder) = Color.decode(decoder.decodeString())
}
