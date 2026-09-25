@file:UseSerializers(DurationSerializer::class)

package de.mineking.hexo.launcher.api

import de.mineking.hexo.launcher.DatabaseConfig
import de.mineking.hexo.launcher.DiscordOAuth2Config
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration

@Serializable
data class ApiApplicationConfig(
    val server: ServerConfig,
    val auth: AuthConfig? = null,
    val oauth2: DiscordOAuth2Config? = null,
    val database: DatabaseConfig? = null,
)

@Serializable
data class ServerConfig(
    val port: Int,
    val webUrl: String,
    val apiUrl: String,
)

@Serializable
data class AuthConfig(
    val secret: String,
    val accessTokenTtl: Duration,
    val refreshTokenTtl: Duration,
)

private object DurationSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder) = Duration.parse(decoder.decodeString())
}
