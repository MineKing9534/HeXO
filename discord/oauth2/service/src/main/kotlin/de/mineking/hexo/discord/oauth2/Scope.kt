package de.mineking.hexo.discord.oauth2

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class Scope(val value: String) {
    Identify("identify"),
    RoleConnectionsWrite("role_connections.write"),
    ;

    override fun toString() = value

    companion object {
        fun of(value: String) = entries.firstOrNull { it.value == value }
    }
}

internal object ScopeListSerializer : KSerializer<List<Scope>> {
    override val descriptor = PrimitiveSerialDescriptor("ScopeListSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<Scope>) = encoder.encodeString(value.joinToString(" "))

    override fun deserialize(decoder: Decoder) = decoder.decodeString()
        .split(" ")
        .mapNotNull { Scope.of(it) }
}
