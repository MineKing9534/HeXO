package de.mineking.hexo.board

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable
sealed class BoardAttribute<T> {
    abstract val default: T
    abstract val serializer: KSerializer<T>

    @Serializable
    data object ShowTurnNumbers : BoardAttribute<Boolean?>() {
        override val default get() = null
        override val serializer = Boolean.serializer().nullable
    }
}

@ConsistentCopyVisibility
data class BoardAttributeValue<T> internal constructor(val key: BoardAttribute<out T>, val value: T)
infix fun <T> BoardAttribute<T>.to(value: T) = BoardAttributeValue(this, value)

fun BoardAttributes(
    vararg values: BoardAttributeValue<*>,
): BoardAttributes = MutableBoardAttributes(values = values)

@Suppress("FunctionNaming")
fun MutableBoardAttributes(
    vararg values: BoardAttributeValue<*>,
) = MutableBoardAttributes(values.associate { (key, value) -> Pair(key, value) }.toMutableMap())

@Serializable(with = BoardAttributesSerializer::class)
interface BoardAttributes {
    val values: Map<BoardAttribute<*>, Any?>

    operator fun <T> get(key: BoardAttribute<T>): T
}

class MutableBoardAttributes(override val values: MutableMap<BoardAttribute<*>, Any?> = mutableMapOf()) : BoardAttributes {
    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: BoardAttribute<T>) = values.getOrElseIfMissing(key) { key.default } as T

    operator fun <T> set(key: BoardAttribute<T>, value: T) {
        values[key] = value
    }

    override fun hashCode() = values.hashCode()
    override fun equals(other: Any?) = other is MutableBoardAttributes && values == other.values
}

fun BoardAttributes.copy() = MutableBoardAttributes(this@copy.values.toMutableMap())

operator fun BoardAttributes.plus(other: BoardAttributes) = MutableBoardAttributes((this@plus.values + other.values).toMutableMap())
operator fun MutableBoardAttributes.plusAssign(other: BoardAttributes) {
    values += other.values
}

internal object BoardAttributesSerializer : KSerializer<BoardAttributes> {
    private val delegate = ListSerializer(BoardAttributeValueSerializer)

    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: BoardAttributes) {
        encoder.encodeSerializableValue(
            delegate,
            value.values.map { (key, value) -> BoardAttributeValue(key, value) },
        )
    }

    override fun deserialize(decoder: Decoder): BoardAttributes {
        return MutableBoardAttributes(
            decoder.decodeSerializableValue(delegate)
                .associate { Pair(it.key, it.value) }
                .toMutableMap(),
        )
    }
}

private object BoardAttributeValueSerializer : KSerializer<BoardAttributeValue<*>> {
    @Suppress("UNCHECKED_CAST")
    private val keySerializer = BoardAttribute.serializer(PolymorphicSerializer(Any::class).nullable) as KSerializer<BoardAttribute<*>>

    @OptIn(InternalSerializationApi::class)
    override val descriptor = buildClassSerialDescriptor("de.mineking.hexo.board.BoardAttributeValue") {
        element("key", keySerializer.descriptor)
        element("value", buildSerialDescriptor("de.mineking.hexo.board.BoardAttributeValue.value", SerialKind.CONTEXTUAL))
    }

    override fun serialize(encoder: Encoder, value: BoardAttributeValue<*>) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, keySerializer, value.key)
        encodeAttributeValue(value.key, value.value)
    }

    private data object MissingValue
    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        lateinit var key: BoardAttribute<*>
        var value: Any? = MissingValue

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> key = decodeSerializableElement(descriptor, 0, keySerializer)
                1 -> value = decodeSerializableElement(descriptor, 1, key.serializer)

                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected index $index")
            }
        }

        if (value is MissingValue) throw SerializationException("Missing board attribute value")

        BoardAttributeValue(key, value)
    }

    private fun <T> CompositeEncoder.encodeAttributeValue(key: BoardAttribute<T>, value: Any?) {
        @Suppress("UNCHECKED_CAST")
        encodeSerializableElement(descriptor, 1, key.serializer, value as T)
    }
}
