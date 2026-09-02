package cc.tyto

import de.mineking.hexo.board.CellCoordinate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal interface ThreatContainer {
    val pv: List<Turn>
}

@JvmInline
@Serializable
internal value class Player(val value: Int)

@Serializable
internal data class Stone(
    val q: Int,
    val r: Int,
    val player: Player,
)

@Serializable
internal enum class SolverEngine {
    @SerialName("idtt") Idtt,
    @SerialName("pns") Pns,
    @SerialName("dfpn") Dfpn,
    @SerialName("pdspn") Pdspn,
}

@Serializable
internal data class SolveRequest(
    @SerialName("win_length") val winLength: Int,
    @SerialName("placement_radius") val placementRadius: Int,
    @SerialName("max_moves") val maxMoves: Int,
    @SerialName("to_move") val toMove: Player,
    @SerialName("moves_remaining") val movesRemaining: Int,
    @SerialName("depth_cap") val depthCap: Int,
    @SerialName("node_budget") val nodeBudget: Int,
    val engine: SolverEngine,
    val wide: Boolean,
    val stones: List<Stone>,
)

@Serializable
internal data class Turn(
    val player: Player,
    val cells: List<CellCoordinate>,
)

@Serializable
internal data class SolveResponse(
    val kind: SolveKind,
    override val pv: List<Turn>,
    val error: String? = null,
) : ThreatContainer

@Serializable
internal enum class SolveKind {
    @SerialName("win") Win,
    @SerialName("no") No,
    @SerialName("budget_exceeded") BudgetExceeded,
    @SerialName("error") Error,
}

@Serializable
internal data class DefenseResponse(
    val kind: DefenseKind,
    val threat: Threat? = null,
    val killers: List<CellCoordinate> = emptyList(),
    @SerialName("pair_anchors") val pairAnchors: List<Pair<CellCoordinate, CellCoordinate>> = emptyList(),
    @SerialName("counter_threats") val counterThreats: List<Pair<CellCoordinate, CellCoordinate>> = emptyList(),
    @SerialName("tactical_pairs") val tacticalPairs: List<Pair<CellCoordinate, CellCoordinate>>,
    @SerialName("unresolved") val unresolved: List<CellCoordinate>,
    @SerialName("best_delay") val bestDelay: CellCoordinate? = null,
    val error: String? = null,
)

@Serializable
internal enum class DefenseKind {
    @SerialName("threat_found") ThreatFound,
    @SerialName("no_threat") NoThreat,
    @SerialName("budget_exceeded") BudgetExceeded,
    @SerialName("error") Error,
}

@Serializable
internal data class Threat(
    override val pv: List<Turn>,
) : ThreatContainer
