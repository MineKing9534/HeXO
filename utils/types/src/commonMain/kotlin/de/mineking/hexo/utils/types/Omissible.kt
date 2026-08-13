package de.mineking.hexo.utils.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

@Serializable(with = OmissibleSerializer::class)
sealed interface Omissible<out T> {
    @JvmInline
    @Serializable
    value class Present<out T>(val value: T) : Omissible<T>

    @Serializable
    data object Omitted : Omissible<Nothing>
}

class OmissibleSerializer<T>(private val childSerializer: KSerializer<T>) : KSerializer<Omissible<T>> {
    override val descriptor: SerialDescriptor = childSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Omissible<T>) {
        when (value) {
            is Omissible.Omitted -> throw SerializationException("Omissible.Omitted cannot be serialized, try using encodeDefaults = false")

            is Omissible.Present -> encoder.encodeInline(descriptor)
                .encodeSerializableValue(childSerializer, value.value)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Omissible<T> {
        if (!descriptor.isNullable && !decoder.decodeNotNullMark()) {
            throw SerializationException("descriptor for ${descriptor.serialName} was not nullable but null mark was encountered")
        }

        val child = decoder.decodeInline(descriptor).decodeSerializableValue(childSerializer)
        return child.present()
    }
}

fun <T> omitted(): Omissible<T> = Omissible.Omitted

fun <T> T.present() = Omissible.Present(this)
inline fun <T> T.omittedIf(predicate: (T) -> Boolean) = if (!predicate(this)) present() else omitted()
fun <T> T?.omittedIfNull() = this?.present() ?: omitted()

@OptIn(ExperimentalContracts::class)
fun <T> Omissible<T>.isPresent(): Boolean {
    contract {
        returns(true) implies (this@isPresent is Omissible.Present<T>)
    }
    return this is Omissible.Present
}

fun <T> Omissible<T>.orElse(other: T) = when {
    isPresent() -> value
    else -> other
}
