package de.mineking.hexo.link.oauth2

import de.mineking.hexo.discord.core.DiscordUserId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class OAuth2Tokens internal constructor(
    private val client: DiscordOAuth2Client,
    internal val data: OAuth2TokensDto,
    val id: DiscordUserId,
) {
    internal fun isExpired() = data.expiresAt + 1.minutes < Clock.System.now()

    internal suspend fun refresh() = client.refreshToken(data.refreshToken)
    internal suspend fun revoke() = client.revokeToken(this)
}

@Serializable
internal data class OAuth2TokensDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresAt: @Serializable(with = ExpiresAtSerializer::class) Instant,
    @SerialName("scope") val scopes: @Serializable(with = ScopeListSerializer::class) List<Scope>,
)

internal object ExpiresAtSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("ExpiresAt", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) = throw UnsupportedOperationException()
    override fun deserialize(decoder: Decoder) = Clock.System.now() + decoder.decodeLong().seconds
}
