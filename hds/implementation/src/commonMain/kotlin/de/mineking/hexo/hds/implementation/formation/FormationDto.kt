package de.mineking.hexo.hds.implementation.formation

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOwner
import de.mineking.hexo.hds.model.Move
import de.mineking.hexo.hds.model.formation.FormationId
import de.mineking.hexo.hds.model.formation.GamePosition
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
internal data class FormationDto(
    val id: FormationId,
    val name: String,
    val gamePosition: GamePositionDto,
)

@Serializable
internal data class GamePositionDto(
    override val currentTurnPlayer: @Serializable(with = FormationCellOwnerSerializer::class) CellOwner,
    override val placementsRemaining: Int,
    val cells: List<GamePositionCell>,
) : GamePosition {
    override val moves = cells
}

@Serializable(with = GamePositionCellSerializer::class)
internal data class GamePositionCell(
    override val coordinate: CellCoordinate,
    override val owner: CellOwner,
) : Move

internal object GamePositionCellSerializer : KSerializer<GamePositionCell> {
    override val descriptor = GamePositionCellDto.serializer().descriptor
    override fun deserialize(decoder: Decoder) = GamePositionCellDto.serializer().deserialize(decoder).let {
        GamePositionCell(
            coordinate = CellCoordinate(it.q, it.r),
            owner = it.player,
        )
    }
    override fun serialize(encoder: Encoder, value: GamePositionCell) = throw UnsupportedOperationException()

    @Serializable
    private data class GamePositionCellDto(
        @SerialName("x") val q: Int,
        @SerialName("y") val r: Int,
        val player: @Serializable(with = FormationCellOwnerSerializer::class) CellOwner,
    )
}

internal object FormationCellOwnerSerializer : KSerializer<CellOwner> {
    override val descriptor = PrimitiveSerialDescriptor("CellOwner", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = when (val value = decoder.decodeString()) {
        "player-1" -> CellOwner.X
        "player-2" -> CellOwner.O
        else -> throw IllegalArgumentException("Unknown cell owner '$value'")
    }

    override fun serialize(encoder: Encoder, value: CellOwner) = throw UnsupportedOperationException()
}
