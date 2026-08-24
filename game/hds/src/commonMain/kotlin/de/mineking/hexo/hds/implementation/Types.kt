package de.mineking.hexo.hds.implementation

import de.mineking.hexo.game.model.LiveDuration
import de.mineking.hexo.game.model.TimeControl
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

internal typealias Duration = @Serializable(with = DurationAsMillisecondsSerializer::class) kotlin.time.Duration
internal typealias Instant = @Serializable(with = InstantAsEpochSerializer::class) kotlin.time.Instant
internal typealias LiveDuration = @Serializable(with = LiveDurationSerializer::class) LiveDuration
internal typealias TimeControl = @Serializable(with = TimeControlSerializer::class) TimeControl

internal object LiveDurationSerializer : KSerializer<LiveDuration> {
    private val delegate = DurationAsMillisecondsSerializer
    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: LiveDuration) = throw UnsupportedOperationException()
    override fun deserialize(decoder: Decoder) = LiveDuration(delegate.deserialize(decoder), Clock.System.now())
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

internal object TimeControlSerializer : KSerializer<TimeControl> {
    private val delegate = TimeControlDto.serializer()
    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: TimeControl) = throw UnsupportedOperationException()
    override fun deserialize(decoder: Decoder) = decoder.decodeSerializableValue(delegate).model
}

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("mode")
@Serializable
internal sealed interface TimeControlDto {
    val model: TimeControl

    @Serializable
    @SerialName("unlimited")
    data object Unlimited : TimeControlDto {
        @Transient
        override val model = TimeControl.Unlimited
    }

    @Serializable
    @SerialName("turn")
    data class Turn(
        @SerialName("turnTimeMs") val turnTime: Duration,
    ) : TimeControlDto {
        @Transient
        override val model = TimeControl.Turn(turnTime)
    }

    @Serializable
    @SerialName("match")
    data class Match(
        @SerialName("mainTimeMs") val mainTime: Duration,
        @SerialName("incrementMs") val increment: Duration,
    ) : TimeControlDto {
        @Transient
        override val model = TimeControl.Match(mainTime, increment)
    }
}
