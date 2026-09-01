@file:JsModule("HeXO-solver/kotlin/pkg/hexo_solver_wasm.js")
@file:JsNonModule

package cc.tyto

import org.khronos.webgl.Int32Array
import kotlin.js.Promise

internal sealed external class Player {
    object P1 : Player
    object P2 : Player
}

internal sealed external class SolveKind {
    object Win : SolveKind
    object No : SolveKind
    object BudgetExceeded : SolveKind
}

internal external class CoordW(
    val q: Int,
    val r: Int,
)

internal external class Turn private constructor() {
    val player: Player
    val cells: Array<CoordW>
}

internal external class SolveOutcome private constructor() {
    val pv: Array<Turn>
    val kind: SolveKind
}

internal sealed external class DefenseKind {
    object ThreatFound : DefenseKind
    object NoThreat : DefenseKind
    object BudgetExceeded : DefenseKind
}

internal external class DefenseOutcome private constructor() {
    val kind: DefenseKind
    val threat: SolveOutcome?
    val killers: Array<CoordW>
    @JsName("pair_anchors") val pairAnchors: Array<PairAnchor>
    @JsName("counter_threats") val counterThreats: Array<PairAnchor>
    @JsName("tactical_pairs") val tacticalPairs: Array<PairAnchor>
    @JsName("unresolved") val unresolved: Array<CoordW>
    @JsName("best_delay") val bestDelay: CoordW?
}

internal external class PairAnchor private constructor() {
    val first: CoordW
    val second: CoordW
}

@JsName("SolverEngineEnum")
sealed external class SolverEngine {
    object Idtt : SolverEngine
    object Pns : SolverEngine
    object Dfpn : SolverEngine
    object Pdspn : SolverEngine
}

internal external class SolverLimits(
    @JsName("depth_cap") val depthCap: Int,
    @JsName("node_budget") val nodeBudget: dynamic,
    val engine: SolverEngine,
)

internal external class Position(
    @JsName("win_length") val winLength: Int,
    @JsName("placement_radius") val placementRadius: Int,
    @JsName("max_moves") val maxMoves: Int,
    @JsName("to_move") val toMove: Player,
    @JsName("moves_remaining") val movesRemaining: Int,
    @JsName("stones_flat") val stonesFlat: Int32Array,
)

internal external class StrixSolver {
    @JsName("solve_wide")
    fun solveWide(position: Position, limits: SolverLimits): SolveOutcome

    @JsName("solve_defense_wide")
    fun solveDefenseWide(position: Position, limits: SolverLimits): DefenseOutcome
}

@JsName("default")
internal external fun init(): Promise<dynamic>
