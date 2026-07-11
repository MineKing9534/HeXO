package de.mineking.hexo.board

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
private data class BoardDto(
    override val cells: Map<CellCoordinate, Cell>,
    override val lineHighlights: List<LineHighlight>,
    override val attributes: BoardAttributes,
) : Board {
    constructor(board: Board) : this(
        cells = board.cells.filter { (_, cell) -> !cell.isEmpty(includeHighlights = true) },
        lineHighlights = board.lineHighlights,
        attributes = board.attributes,
    )
}

internal object BoardSerializer : KSerializer<Board> {
    override val descriptor = BoardDto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Board) = BoardDto.serializer().serialize(encoder, BoardDto(value))
    override fun deserialize(decoder: Decoder): Board = BoardDto.serializer().deserialize(decoder)
}

internal object CellSerializer : KSerializer<Cell> {
    override val descriptor = MutableCell.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Cell) = MutableCell.serializer().serialize(encoder, value.copy())
    override fun deserialize(decoder: Decoder) = MutableCell.serializer().deserialize(decoder)
}
