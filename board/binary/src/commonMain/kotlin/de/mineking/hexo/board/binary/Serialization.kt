package de.mineking.hexo.board.binary

import de.mineking.hexo.board.Board
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
private data class BoardDto(val encoded: String)

typealias SerializableBoard = @Serializable(with = BinaryBoardSerializer::class) Board

object BinaryBoardSerializer : KSerializer<Board> {
    private val delegate = BoardDto.serializer()
    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder) = BoardBinary.decodeFromString(delegate.deserialize(decoder).encoded)
    override fun serialize(encoder: Encoder, value: Board) = delegate.serialize(encoder, BoardDto(BoardBinary.encodeToString(value)))
}
