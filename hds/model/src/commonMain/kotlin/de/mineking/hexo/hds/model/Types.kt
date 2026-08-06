package de.mineking.hexo.hds.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

typealias Duration = @Serializable(with = DurationAsMillisecondsSerializer::class) kotlin.time.Duration
typealias Instant = @Serializable(with = InstantAsEpochSerializer::class) kotlin.time.Instant

@Serializable(with = LiveDurationSerializer::class)
data class LiveDuration(val duration: Duration, val timestamp: Instant)

internal object LiveDurationSerializer : KSerializer<LiveDuration> {
    private val delegate = DurationAsMillisecondsSerializer

    override val descriptor = delegate.descriptor
    override fun deserialize(decoder: Decoder) = LiveDuration(delegate.deserialize(decoder), Clock.System.now())
    override fun serialize(encoder: Encoder, value: LiveDuration) = throw UnsupportedOperationException()
}

internal object DurationAsMillisecondsSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) = encoder.encodeLong(value.inWholeMilliseconds)
    override fun deserialize(decoder: Decoder) = decoder.decodeLong().milliseconds
}

internal object InstantAsEpochSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.toEpochMilliseconds())
    override fun deserialize(decoder: Decoder) = Instant.fromEpochMilliseconds(decoder.decodeLong())
}
