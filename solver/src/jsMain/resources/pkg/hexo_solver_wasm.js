/* @ts-self-types="./hexo_solver_wasm.d.ts" */

/**
 * Measurements recomputed by the independent all-defense certificate verifier.
 */
export class CertificateVerification {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(CertificateVerification.prototype);
        obj.__wbg_ptr = ptr;
        CertificateVerificationFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        CertificateVerificationFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_certificateverification_free(ptr, 0);
    }
    /**
     * @returns {bigint}
     */
    get dag_nodes() {
        const ret = wasm.__wbg_get_certificateverification_dag_nodes(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * @returns {number}
     */
    get max_attacker_turns() {
        const ret = wasm.__wbg_get_certificateverification_max_attacker_turns(this.__wbg_ptr);
        return ret >>> 0;
    }
    /**
     * @returns {bigint}
     */
    get proof_edges() {
        const ret = wasm.__wbg_get_certificateverification_proof_edges(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * @param {bigint} arg0
     */
    set dag_nodes(arg0) {
        wasm.__wbg_set_certificateverification_dag_nodes(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set max_attacker_turns(arg0) {
        wasm.__wbg_set_certificateverification_max_attacker_turns(this.__wbg_ptr, arg0);
    }
    /**
     * @param {bigint} arg0
     */
    set proof_edges(arg0) {
        wasm.__wbg_set_certificateverification_proof_edges(this.__wbg_ptr, arg0);
    }
}
if (Symbol.dispose) CertificateVerification.prototype[Symbol.dispose] = CertificateVerification.prototype.free;

export class CoordW {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(CoordW.prototype);
        obj.__wbg_ptr = ptr;
        CoordWFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    static __unwrap(jsValue) {
        if (!(jsValue instanceof CoordW)) {
            return 0;
        }
        return jsValue.__destroy_into_raw();
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        CoordWFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_coordw_free(ptr, 0);
    }
    /**
     * @param {number} q
     * @param {number} r
     */
    constructor(q, r) {
        const ret = wasm.coordw_new(q, r);
        this.__wbg_ptr = ret >>> 0;
        CoordWFinalization.register(this, this.__wbg_ptr, this);
        return this;
    }
    /**
     * @returns {number}
     */
    get q() {
        const ret = wasm.__wbg_get_coordw_q(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {number}
     */
    get r() {
        const ret = wasm.__wbg_get_coordw_r(this.__wbg_ptr);
        return ret;
    }
    /**
     * @param {number} arg0
     */
    set q(arg0) {
        wasm.__wbg_set_coordw_q(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set r(arg0) {
        wasm.__wbg_set_coordw_r(this.__wbg_ptr, arg0);
    }
}
if (Symbol.dispose) CoordW.prototype[Symbol.dispose] = CoordW.prototype.free;

/**
 * @enum {0 | 1 | 2}
 */
export const DefenseKind = Object.freeze({
    /**
     * The opponent has a proven forcing threat (the mover has a defense to find).
     */
    ThreatFound: 0, "0": "ThreatFound",
    /**
     * PROVEN no opponent threat within `depth_cap` — nothing to defend.
     * (Historically this also covered budget starvation; that case is now
     * reported as `BudgetExceeded`.)
     */
    NoThreat: 1, "1": "NoThreat",
    /**
     * The threat check exhausted `node_budget` (or the position was not
     * analyzable: invalid turn shape / pathological coordinate spread) before
     * proving anything. NOT a safety verdict — retry with a larger budget.
     */
    BudgetExceeded: 2, "2": "BudgetExceeded",
});

export class DefenseOutcome {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(DefenseOutcome.prototype);
        obj.__wbg_ptr = ptr;
        DefenseOutcomeFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        DefenseOutcomeFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_defenseoutcome_free(ptr, 0);
    }
    /**
     * Max-delay fallback: the surviving candidate whose re-proven threat PV is
     * longest. `None` when the threat PV offers no legal candidate.
     * @returns {CoordW | undefined}
     */
    get best_delay() {
        const ret = wasm.__wbg_get_defenseoutcome_best_delay(this.__wbg_ptr);
        return ret === 0 ? undefined : CoordW.__wrap(ret);
    }
    /**
     * Pair defenses that work by creating the mover's own forcing threat and
     * taking initiative before the opponent can start its hypothetical line.
     * @returns {PairAnchor[]}
     */
    get counter_threats() {
        const ret = wasm.__wbg_get_defenseoutcome_counter_threats(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * Single placements after which the threat is no longer provable (or which
     * end the game outright).
     * @returns {CoordW[]}
     */
    get killers() {
        const ret = wasm.__wbg_get_defenseoutcome_killers(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * `ThreatFound` if the opponent has a proven threat, else `NoThreat`.
     * @returns {DefenseKind}
     */
    get kind() {
        const ret = wasm.__wbg_get_defenseoutcome_kind(this.__wbg_ptr);
        return ret;
    }
    /**
     * Total search nodes across the threat solve + all candidate re-solves.
     * `DefenseAnalysis` does not currently aggregate node counts, so this is 0
     * (a future enhancement could propagate the sum).
     * @returns {bigint}
     */
    get nodes() {
        const ret = wasm.__wbg_get_defenseoutcome_nodes(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * `(first, second)` placement pairs that jointly refute the threat.
     * Searched only when the mover has 2 placements left and `killers` is empty.
     * @returns {PairAnchor[]}
     */
    get pair_anchors() {
        const ret = wasm.__wbg_get_defenseoutcome_pair_anchors(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * Exact minimum-cover pairs for the immediate attack whose deeper
     * whole-line verification remains inconclusive.
     * @returns {PairAnchor[]}
     */
    get tactical_pairs() {
        const ret = wasm.__wbg_get_defenseoutcome_tactical_pairs(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * The opponent's threat as a `SolveOutcome` (`kind: Win`), chunked into
     * turns the same way as `solve`: the threat is a fresh 2-placement turn
     * for the opponent, so the first turn has 2 cells. `None` when there is no
     * proven threat; always present under `ThreatFound`, but its `pv` may be
     * EMPTY when the PV re-derivation starved under `node_budget` (the win and
     * its `depth` are still proven — retry with a larger budget to recover the
     * line. PV-blocking candidates may be unavailable in that case, but
     * counter-threat pairs are generated independently from the real mover's
     * position). `nodes` is 0 (the
     * sub-solve's node count is not surfaced by `solve_defense`).
     * @returns {SolveOutcome | undefined}
     */
    get threat() {
        const ret = wasm.__wbg_get_defenseoutcome_threat(this.__wbg_ptr);
        return ret === 0 ? undefined : SolveOutcome.__wrap(ret);
    }
    /**
     * Wall-clock time of the whole analysis (ms). `0.0` on wasm (no monotonic
     * clock — see the module-level `Instant::now()` note).
     * @returns {number}
     */
    get time_ms() {
        const ret = wasm.__wbg_get_defenseoutcome_time_ms(this.__wbg_ptr);
        return ret;
    }
    /**
     * Candidate replies whose deeper verification exhausted the work budget.
     * They are not certified defenses, but remain tactically relevant.
     * @returns {CoordW[]}
     */
    get unresolved() {
        const ret = wasm.__wbg_get_defenseoutcome_unresolved(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * Max-delay fallback: the surviving candidate whose re-proven threat PV is
     * longest. `None` when the threat PV offers no legal candidate.
     * @param {CoordW | null} [arg0]
     */
    set best_delay(arg0) {
        let ptr0 = 0;
        if (!isLikeNone(arg0)) {
            _assertClass(arg0, CoordW);
            ptr0 = arg0.__destroy_into_raw();
        }
        wasm.__wbg_set_defenseoutcome_best_delay(this.__wbg_ptr, ptr0);
    }
    /**
     * Pair defenses that work by creating the mover's own forcing threat and
     * taking initiative before the opponent can start its hypothetical line.
     * @param {PairAnchor[]} arg0
     */
    set counter_threats(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_defenseoutcome_counter_threats(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * Single placements after which the threat is no longer provable (or which
     * end the game outright).
     * @param {CoordW[]} arg0
     */
    set killers(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_defenseoutcome_killers(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * `ThreatFound` if the opponent has a proven threat, else `NoThreat`.
     * @param {DefenseKind} arg0
     */
    set kind(arg0) {
        wasm.__wbg_set_defenseoutcome_kind(this.__wbg_ptr, arg0);
    }
    /**
     * Total search nodes across the threat solve + all candidate re-solves.
     * `DefenseAnalysis` does not currently aggregate node counts, so this is 0
     * (a future enhancement could propagate the sum).
     * @param {bigint} arg0
     */
    set nodes(arg0) {
        wasm.__wbg_set_defenseoutcome_nodes(this.__wbg_ptr, arg0);
    }
    /**
     * `(first, second)` placement pairs that jointly refute the threat.
     * Searched only when the mover has 2 placements left and `killers` is empty.
     * @param {PairAnchor[]} arg0
     */
    set pair_anchors(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_defenseoutcome_pair_anchors(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * Exact minimum-cover pairs for the immediate attack whose deeper
     * whole-line verification remains inconclusive.
     * @param {PairAnchor[]} arg0
     */
    set tactical_pairs(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_defenseoutcome_tactical_pairs(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * The opponent's threat as a `SolveOutcome` (`kind: Win`), chunked into
     * turns the same way as `solve`: the threat is a fresh 2-placement turn
     * for the opponent, so the first turn has 2 cells. `None` when there is no
     * proven threat; always present under `ThreatFound`, but its `pv` may be
     * EMPTY when the PV re-derivation starved under `node_budget` (the win and
     * its `depth` are still proven — retry with a larger budget to recover the
     * line. PV-blocking candidates may be unavailable in that case, but
     * counter-threat pairs are generated independently from the real mover's
     * position). `nodes` is 0 (the
     * sub-solve's node count is not surfaced by `solve_defense`).
     * @param {SolveOutcome | null} [arg0]
     */
    set threat(arg0) {
        let ptr0 = 0;
        if (!isLikeNone(arg0)) {
            _assertClass(arg0, SolveOutcome);
            ptr0 = arg0.__destroy_into_raw();
        }
        wasm.__wbg_set_defenseoutcome_threat(this.__wbg_ptr, ptr0);
    }
    /**
     * Wall-clock time of the whole analysis (ms). `0.0` on wasm (no monotonic
     * clock — see the module-level `Instant::now()` note).
     * @param {number} arg0
     */
    set time_ms(arg0) {
        wasm.__wbg_set_defenseoutcome_time_ms(this.__wbg_ptr, arg0);
    }
    /**
     * Candidate replies whose deeper verification exhausted the work budget.
     * They are not certified defenses, but remain tactically relevant.
     * @param {CoordW[]} arg0
     */
    set unresolved(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_defenseoutcome_unresolved(this.__wbg_ptr, ptr0, len0);
    }
}
if (Symbol.dispose) DefenseOutcome.prototype[Symbol.dispose] = DefenseOutcome.prototype.free;

/**
 * One exact two-stone minimum cover after a fixed attacker turn.
 */
export class MinimumDefenseCover {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(MinimumDefenseCover.prototype);
        obj.__wbg_ptr = ptr;
        MinimumDefenseCoverFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    static __unwrap(jsValue) {
        if (!(jsValue instanceof MinimumDefenseCover)) {
            return 0;
        }
        return jsValue.__destroy_into_raw();
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        MinimumDefenseCoverFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_minimumdefensecover_free(ptr, 0);
    }
    /**
     * @returns {CoordW}
     */
    get first() {
        const ret = wasm.__wbg_get_minimumdefensecover_first(this.__wbg_ptr);
        return CoordW.__wrap(ret);
    }
    /**
     * @returns {CoordW}
     */
    get second() {
        const ret = wasm.__wbg_get_minimumdefensecover_second(this.__wbg_ptr);
        return CoordW.__wrap(ret);
    }
    /**
     * @param {CoordW} arg0
     */
    set first(arg0) {
        _assertClass(arg0, CoordW);
        var ptr0 = arg0.__destroy_into_raw();
        wasm.__wbg_set_minimumdefensecover_first(this.__wbg_ptr, ptr0);
    }
    /**
     * @param {CoordW} arg0
     */
    set second(arg0) {
        _assertClass(arg0, CoordW);
        var ptr0 = arg0.__destroy_into_raw();
        wasm.__wbg_set_minimumdefensecover_second(this.__wbg_ptr, ptr0);
    }
}
if (Symbol.dispose) MinimumDefenseCover.prototype[Symbol.dispose] = MinimumDefenseCover.prototype.free;

/**
 * Classification of the fixed attacker turn used by the replay defence prover.
 * @enum {0 | 1 | 2}
 */
export const MinimumDefenseKind = Object.freeze({
    Covers: 0, "0": "Covers",
    AttackerWin: 1, "1": "AttackerWin",
    NotForcing: 2, "2": "NotForcing",
});

export class MinimumDefenseOutcome {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(MinimumDefenseOutcome.prototype);
        obj.__wbg_ptr = ptr;
        MinimumDefenseOutcomeFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        MinimumDefenseOutcomeFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_minimumdefenseoutcome_free(ptr, 0);
    }
    /**
     * @returns {MinimumDefenseCover[]}
     */
    get covers() {
        const ret = wasm.__wbg_get_minimumdefenseoutcome_covers(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * @returns {MinimumDefenseKind}
     */
    get kind() {
        const ret = wasm.__wbg_get_minimumdefenseoutcome_kind(this.__wbg_ptr);
        return ret;
    }
    /**
     * @param {MinimumDefenseCover[]} arg0
     */
    set covers(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_minimumdefenseoutcome_covers(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * @param {MinimumDefenseKind} arg0
     */
    set kind(arg0) {
        wasm.__wbg_set_minimumdefenseoutcome_kind(this.__wbg_ptr, arg0);
    }
}
if (Symbol.dispose) MinimumDefenseOutcome.prototype[Symbol.dispose] = MinimumDefenseOutcome.prototype.free;

export class PairAnchor {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(PairAnchor.prototype);
        obj.__wbg_ptr = ptr;
        PairAnchorFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    static __unwrap(jsValue) {
        if (!(jsValue instanceof PairAnchor)) {
            return 0;
        }
        return jsValue.__destroy_into_raw();
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        PairAnchorFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_pairanchor_free(ptr, 0);
    }
    /**
     * @returns {CoordW}
     */
    get first() {
        const ret = wasm.__wbg_get_pairanchor_first(this.__wbg_ptr);
        return CoordW.__wrap(ret);
    }
    /**
     * @returns {CoordW}
     */
    get second() {
        const ret = wasm.__wbg_get_pairanchor_second(this.__wbg_ptr);
        return CoordW.__wrap(ret);
    }
    /**
     * @param {CoordW} arg0
     */
    set first(arg0) {
        _assertClass(arg0, CoordW);
        var ptr0 = arg0.__destroy_into_raw();
        wasm.__wbg_set_pairanchor_first(this.__wbg_ptr, ptr0);
    }
    /**
     * @param {CoordW} arg0
     */
    set second(arg0) {
        _assertClass(arg0, CoordW);
        var ptr0 = arg0.__destroy_into_raw();
        wasm.__wbg_set_pairanchor_second(this.__wbg_ptr, ptr0);
    }
}
if (Symbol.dispose) PairAnchor.prototype[Symbol.dispose] = PairAnchor.prototype.free;

/**
 * @enum {1 | 2}
 */
export const Player = Object.freeze({
    P1: 1, "1": "P1",
    P2: 2, "2": "P2",
});

export class Position {
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        PositionFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_position_free(ptr, 0);
    }
    /**
     * @returns {number}
     */
    get max_moves() {
        const ret = wasm.__wbg_get_position_max_moves(this.__wbg_ptr);
        return ret >>> 0;
    }
    /**
     * @returns {number}
     */
    get moves_remaining() {
        const ret = wasm.__wbg_get_position_moves_remaining(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {number}
     */
    get placement_radius() {
        const ret = wasm.__wbg_get_position_placement_radius(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {Stone[]}
     */
    get stones() {
        const ret = wasm.__wbg_get_position_stones(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * @returns {Player}
     */
    get to_move() {
        const ret = wasm.__wbg_get_position_to_move(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {number}
     */
    get win_length() {
        const ret = wasm.__wbg_get_position_win_length(this.__wbg_ptr);
        return ret;
    }
    /**
     * Construct a position. `stones_flat` is a flat `[q, r, player, q, r, player, ...]`
     * array (player = 1 for P1, 2 for P2); any trailing partial triple is ignored.
     * @param {number} win_length
     * @param {number} placement_radius
     * @param {number} max_moves
     * @param {Player} to_move
     * @param {number} moves_remaining
     * @param {Int32Array} stones_flat
     */
    constructor(win_length, placement_radius, max_moves, to_move, moves_remaining, stones_flat) {
        const ptr0 = passArray32ToWasm0(stones_flat, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        const ret = wasm.position_new(win_length, placement_radius, max_moves, to_move, moves_remaining, ptr0, len0);
        this.__wbg_ptr = ret >>> 0;
        PositionFinalization.register(this, this.__wbg_ptr, this);
        return this;
    }
    /**
     * @param {number} arg0
     */
    set max_moves(arg0) {
        wasm.__wbg_set_position_max_moves(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set moves_remaining(arg0) {
        wasm.__wbg_set_position_moves_remaining(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set placement_radius(arg0) {
        wasm.__wbg_set_position_placement_radius(this.__wbg_ptr, arg0);
    }
    /**
     * @param {Stone[]} arg0
     */
    set stones(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_position_stones(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * @param {Player} arg0
     */
    set to_move(arg0) {
        wasm.__wbg_set_position_to_move(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set win_length(arg0) {
        wasm.__wbg_set_position_win_length(this.__wbg_ptr, arg0);
    }
}
if (Symbol.dispose) Position.prototype[Symbol.dispose] = Position.prototype.free;

/**
 * Result of certificate-guided, depth-bounded PDS-PN optimization. `Win`
 * means the adjacent lower/upper bounds certify the exact shortest attacker
 * depth; `BudgetExceeded` can still carry useful verified bounds and a best PV.
 */
export class ShortestOutcome {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(ShortestOutcome.prototype);
        obj.__wbg_ptr = ptr;
        ShortestOutcomeFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        ShortestOutcomeFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_shortestoutcome_free(ptr, 0);
    }
    /**
     * @returns {number}
     */
    get best_upper_depth() {
        const ret = wasm.__wbg_get_shortestoutcome_best_upper_depth(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {bigint}
     */
    get certificate_edges() {
        const ret = wasm.__wbg_get_shortestoutcome_certificate_edges(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * @returns {string}
     */
    get certificate_json() {
        let deferred1_0;
        let deferred1_1;
        try {
            const ret = wasm.__wbg_get_shortestoutcome_certificate_json(this.__wbg_ptr);
            deferred1_0 = ret[0];
            deferred1_1 = ret[1];
            return getStringFromWasm0(ret[0], ret[1]);
        } finally {
            wasm.__wbindgen_free(deferred1_0, deferred1_1, 1);
        }
    }
    /**
     * @returns {number}
     */
    get certificate_max_attacker_turns() {
        const ret = wasm.__wbg_get_shortestoutcome_certificate_max_attacker_turns(this.__wbg_ptr);
        return ret >>> 0;
    }
    /**
     * @returns {bigint}
     */
    get certificate_nodes() {
        const ret = wasm.__wbg_get_shortestoutcome_certificate_nodes(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * @returns {number}
     */
    get depth() {
        const ret = wasm.__wbg_get_shortestoutcome_depth(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {number}
     */
    get excluded_through_depth() {
        const ret = wasm.__wbg_get_shortestoutcome_excluded_through_depth(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {SolveKind}
     */
    get kind() {
        const ret = wasm.__wbg_get_shortestoutcome_kind(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {bigint}
     */
    get nodes() {
        const ret = wasm.__wbg_get_shortestoutcome_nodes(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * @returns {Turn[]}
     */
    get pv() {
        const ret = wasm.__wbg_get_shortestoutcome_pv(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * @returns {boolean}
     */
    get shortest_certified() {
        const ret = wasm.__wbg_get_shortestoutcome_shortest_certified(this.__wbg_ptr);
        return ret !== 0;
    }
    /**
     * @returns {bigint}
     */
    get threshold_probes() {
        const ret = wasm.__wbg_get_shortestoutcome_threshold_probes(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * @returns {number}
     */
    get time_ms() {
        const ret = wasm.__wbg_get_shortestoutcome_time_ms(this.__wbg_ptr);
        return ret;
    }
    /**
     * @param {number} arg0
     */
    set best_upper_depth(arg0) {
        wasm.__wbg_set_shortestoutcome_best_upper_depth(this.__wbg_ptr, arg0);
    }
    /**
     * @param {bigint} arg0
     */
    set certificate_edges(arg0) {
        wasm.__wbg_set_shortestoutcome_certificate_edges(this.__wbg_ptr, arg0);
    }
    /**
     * @param {string} arg0
     */
    set certificate_json(arg0) {
        const ptr0 = passStringToWasm0(arg0, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_shortestoutcome_certificate_json(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * @param {number} arg0
     */
    set certificate_max_attacker_turns(arg0) {
        wasm.__wbg_set_shortestoutcome_certificate_max_attacker_turns(this.__wbg_ptr, arg0);
    }
    /**
     * @param {bigint} arg0
     */
    set certificate_nodes(arg0) {
        wasm.__wbg_set_shortestoutcome_certificate_nodes(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set depth(arg0) {
        wasm.__wbg_set_shortestoutcome_depth(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set excluded_through_depth(arg0) {
        wasm.__wbg_set_shortestoutcome_excluded_through_depth(this.__wbg_ptr, arg0);
    }
    /**
     * @param {SolveKind} arg0
     */
    set kind(arg0) {
        wasm.__wbg_set_shortestoutcome_kind(this.__wbg_ptr, arg0);
    }
    /**
     * @param {bigint} arg0
     */
    set nodes(arg0) {
        wasm.__wbg_set_shortestoutcome_nodes(this.__wbg_ptr, arg0);
    }
    /**
     * @param {Turn[]} arg0
     */
    set pv(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_shortestoutcome_pv(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * @param {boolean} arg0
     */
    set shortest_certified(arg0) {
        wasm.__wbg_set_shortestoutcome_shortest_certified(this.__wbg_ptr, arg0);
    }
    /**
     * @param {bigint} arg0
     */
    set threshold_probes(arg0) {
        wasm.__wbg_set_shortestoutcome_threshold_probes(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set time_ms(arg0) {
        wasm.__wbg_set_shortestoutcome_time_ms(this.__wbg_ptr, arg0);
    }
}
if (Symbol.dispose) ShortestOutcome.prototype[Symbol.dispose] = ShortestOutcome.prototype.free;

/**
 * @enum {0 | 1 | 2}
 */
export const SolveKind = Object.freeze({
    Win: 0, "0": "Win",
    No: 1, "1": "No",
    BudgetExceeded: 2, "2": "BudgetExceeded",
});

export class SolveOutcome {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(SolveOutcome.prototype);
        obj.__wbg_ptr = ptr;
        SolveOutcomeFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        SolveOutcomeFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_solveoutcome_free(ptr, 0);
    }
    /**
     * @returns {bigint}
     */
    get certificate_edges() {
        const ret = wasm.__wbg_get_solveoutcome_certificate_edges(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * Pretty JSON for a replay-verified PDS-PN all-defense DAG; empty for other
     * engines/verdicts or if optional reconstruction failed.
     * @returns {string}
     */
    get certificate_json() {
        let deferred1_0;
        let deferred1_1;
        try {
            const ret = wasm.__wbg_get_solveoutcome_certificate_json(this.__wbg_ptr);
            deferred1_0 = ret[0];
            deferred1_1 = ret[1];
            return getStringFromWasm0(ret[0], ret[1]);
        } finally {
            wasm.__wbindgen_free(deferred1_0, deferred1_1, 1);
        }
    }
    /**
     * @returns {number}
     */
    get certificate_max_attacker_turns() {
        const ret = wasm.__wbg_get_solveoutcome_certificate_max_attacker_turns(this.__wbg_ptr);
        return ret >>> 0;
    }
    /**
     * @returns {bigint}
     */
    get certificate_nodes() {
        const ret = wasm.__wbg_get_solveoutcome_certificate_nodes(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * @returns {number}
     */
    get depth() {
        const ret = wasm.__wbg_get_solveoutcome_depth(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {SolveKind}
     */
    get kind() {
        const ret = wasm.__wbg_get_solveoutcome_kind(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {bigint}
     */
    get nodes() {
        const ret = wasm.__wbg_get_solveoutcome_nodes(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * @returns {Turn[]}
     */
    get pv() {
        const ret = wasm.__wbg_get_solveoutcome_pv(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * @returns {number}
     */
    get time_ms() {
        const ret = wasm.__wbg_get_solveoutcome_time_ms(this.__wbg_ptr);
        return ret;
    }
    /**
     * @param {bigint} arg0
     */
    set certificate_edges(arg0) {
        wasm.__wbg_set_solveoutcome_certificate_edges(this.__wbg_ptr, arg0);
    }
    /**
     * Pretty JSON for a replay-verified PDS-PN all-defense DAG; empty for other
     * engines/verdicts or if optional reconstruction failed.
     * @param {string} arg0
     */
    set certificate_json(arg0) {
        const ptr0 = passStringToWasm0(arg0, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_solveoutcome_certificate_json(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * @param {number} arg0
     */
    set certificate_max_attacker_turns(arg0) {
        wasm.__wbg_set_solveoutcome_certificate_max_attacker_turns(this.__wbg_ptr, arg0);
    }
    /**
     * @param {bigint} arg0
     */
    set certificate_nodes(arg0) {
        wasm.__wbg_set_solveoutcome_certificate_nodes(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set depth(arg0) {
        wasm.__wbg_set_solveoutcome_depth(this.__wbg_ptr, arg0);
    }
    /**
     * @param {SolveKind} arg0
     */
    set kind(arg0) {
        wasm.__wbg_set_solveoutcome_kind(this.__wbg_ptr, arg0);
    }
    /**
     * @param {bigint} arg0
     */
    set nodes(arg0) {
        wasm.__wbg_set_solveoutcome_nodes(this.__wbg_ptr, arg0);
    }
    /**
     * @param {Turn[]} arg0
     */
    set pv(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_solveoutcome_pv(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * @param {number} arg0
     */
    set time_ms(arg0) {
        wasm.__wbg_set_solveoutcome_time_ms(this.__wbg_ptr, arg0);
    }
}
if (Symbol.dispose) SolveOutcome.prototype[Symbol.dispose] = SolveOutcome.prototype.free;

/**
 * @enum {0 | 1 | 2 | 3}
 */
export const SolverEngineEnum = Object.freeze({
    Idtt: 0, "0": "Idtt",
    Pns: 1, "1": "Pns",
    Dfpn: 2, "2": "Dfpn",
    Pdspn: 3, "3": "Pdspn",
});

export class SolverLimits {
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        SolverLimitsFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_solverlimits_free(ptr, 0);
    }
    /**
     * IDTT attacker-turn horizon. For DFPN/PDS-PN this only bounds fallback
     * principal-variation recovery; their main proof is node-budget driven.
     * @returns {number}
     */
    get depth_cap() {
        const ret = wasm.__wbg_get_solverlimits_depth_cap(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {SolverEngineEnum}
     */
    get engine() {
        const ret = wasm.__wbg_get_solverlimits_engine(this.__wbg_ptr);
        return ret;
    }
    /**
     * Maximum engine work counter. Node meanings are engine-specific.
     * @returns {bigint}
     */
    get node_budget() {
        const ret = wasm.__wbg_get_solverlimits_node_budget(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * PDS-PN level-2 expansions per newly reached frontier. Ignored by the
     * other engines. This is separate from the outer `node_budget`.
     * @returns {bigint}
     */
    get pn2_nodes() {
        const ret = wasm.__wbg_get_solverlimits_pn2_nodes(this.__wbg_ptr);
        return BigInt.asUintN(64, ret);
    }
    /**
     * IDTT attacker-turn horizon. For DFPN/PDS-PN this only bounds fallback
     * principal-variation recovery; their main proof is node-budget driven.
     * @param {number} arg0
     */
    set depth_cap(arg0) {
        wasm.__wbg_set_solverlimits_depth_cap(this.__wbg_ptr, arg0);
    }
    /**
     * @param {SolverEngineEnum} arg0
     */
    set engine(arg0) {
        wasm.__wbg_set_solverlimits_engine(this.__wbg_ptr, arg0);
    }
    /**
     * Maximum engine work counter. Node meanings are engine-specific.
     * @param {bigint} arg0
     */
    set node_budget(arg0) {
        wasm.__wbg_set_solverlimits_node_budget(this.__wbg_ptr, arg0);
    }
    /**
     * PDS-PN level-2 expansions per newly reached frontier. Ignored by the
     * other engines. This is separate from the outer `node_budget`.
     * @param {bigint} arg0
     */
    set pn2_nodes(arg0) {
        wasm.__wbg_set_solverlimits_pn2_nodes(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} depth_cap
     * @param {bigint} node_budget
     * @param {SolverEngineEnum} engine
     */
    constructor(depth_cap, node_budget, engine) {
        const ret = wasm.solverlimits_new(depth_cap, node_budget, engine);
        this.__wbg_ptr = ret >>> 0;
        SolverLimitsFinalization.register(this, this.__wbg_ptr, this);
        return this;
    }
}
if (Symbol.dispose) SolverLimits.prototype[Symbol.dispose] = SolverLimits.prototype.free;

export class Stone {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(Stone.prototype);
        obj.__wbg_ptr = ptr;
        StoneFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    static __unwrap(jsValue) {
        if (!(jsValue instanceof Stone)) {
            return 0;
        }
        return jsValue.__destroy_into_raw();
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        StoneFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_stone_free(ptr, 0);
    }
    /**
     * @returns {CoordW}
     */
    get coord() {
        const ret = wasm.__wbg_get_stone_coord(this.__wbg_ptr);
        return CoordW.__wrap(ret);
    }
    /**
     * @returns {Player}
     */
    get player() {
        const ret = wasm.__wbg_get_stone_player(this.__wbg_ptr);
        return ret;
    }
    /**
     * @param {CoordW} arg0
     */
    set coord(arg0) {
        _assertClass(arg0, CoordW);
        var ptr0 = arg0.__destroy_into_raw();
        wasm.__wbg_set_stone_coord(this.__wbg_ptr, ptr0);
    }
    /**
     * @param {Player} arg0
     */
    set player(arg0) {
        wasm.__wbg_set_stone_player(this.__wbg_ptr, arg0);
    }
    /**
     * @param {CoordW} coord
     * @param {Player} player
     */
    constructor(coord, player) {
        _assertClass(coord, CoordW);
        var ptr0 = coord.__destroy_into_raw();
        const ret = wasm.stone_new(ptr0, player);
        this.__wbg_ptr = ret >>> 0;
        StoneFinalization.register(this, this.__wbg_ptr, this);
        return this;
    }
}
if (Symbol.dispose) Stone.prototype[Symbol.dispose] = Stone.prototype.free;

export class StrixSolver {
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        StrixSolverFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_strixsolver_free(ptr, 0);
    }
    /**
     * Enumerate every exact minimum cover after an attacker turn that has
     * already been placed. `position.to_move` identifies that attacker even
     * though the historical game is now waiting for the defender's reply.
     * @param {Position} position
     * @param {boolean} wide
     * @returns {MinimumDefenseOutcome}
     */
    minimum_defenses_after_attack(position, wide) {
        _assertClass(position, Position);
        const ret = wasm.strixsolver_minimum_defenses_after_attack(this.__wbg_ptr, position.__wbg_ptr, wide);
        if (ret[2]) {
            throw takeFromExternrefTable0(ret[1]);
        }
        return MinimumDefenseOutcome.__wrap(ret[0]);
    }
    constructor() {
        const ret = wasm.strixsolver_new();
        this.__wbg_ptr = ret >>> 0;
        StrixSolverFinalization.register(this, this.__wbg_ptr, this);
        return this;
    }
    /**
     * Tighten a verified PDS-PN proof to the shortest attacker-turn bound by
     * binary-searching horizons with depth-bounded PDS-PN. Runs synchronously;
     * browser callers invoke it inside the cancellable solver Web Worker.
     * @param {Position} position
     * @param {SolverLimits} limits
     * @param {string} certificate_json
     * @param {boolean} wide
     * @returns {ShortestOutcome}
     */
    optimize_certificate(position, limits, certificate_json, wide) {
        _assertClass(position, Position);
        _assertClass(limits, SolverLimits);
        const ptr0 = passStringToWasm0(certificate_json, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
        const len0 = WASM_VECTOR_LEN;
        const ret = wasm.strixsolver_optimize_certificate(this.__wbg_ptr, position.__wbg_ptr, limits.__wbg_ptr, ptr0, len0, wide);
        if (ret[2]) {
            throw takeFromExternrefTable0(ret[1]);
        }
        return ShortestOutcome.__wrap(ret[0]);
    }
    /**
     * @param {Position} position
     * @param {SolverLimits} limits
     * @returns {SolveOutcome}
     */
    solve(position, limits) {
        _assertClass(position, Position);
        _assertClass(limits, SolverLimits);
        const ret = wasm.strixsolver_solve(this.__wbg_ptr, position.__wbg_ptr, limits.__wbg_ptr);
        if (ret[2]) {
            throw takeFromExternrefTable0(ret[1]);
        }
        return SolveOutcome.__wrap(ret[0]);
    }
    /**
     * Defensive analysis for the side to move: detects the opponent's
     * flipped-perspective forcing threat and reports which placements refute it
     * (killers / pair anchors / best-delay fallback). Wraps
     * `hexo_solver::solve_defense_verdict_from_position`, which delegates to
     * the battle-tested `forcing::solve_defense_ex`. Uses the TIGHT generator;
     * see `solve_defense_wide` for threats the tight generator cannot see.
     * A starved threat check reports `kind: BudgetExceeded`, distinct from the
     * proven `NoThreat`.
     *
     * `limits.engine` is INERT for defense: the analysis always uses the forcing
     * (idtt) solver internally (the candidate-verification pattern is
     * forcing-only). `depth_cap` and `node_budget` are honored. The time limit
     * is a fixed 10s default on native (the `SolverLimits` surface has no time
     * field); on wasm the deadline is skipped and `node_budget` is the sole
     * bound.
     *
     * Requires a GAME-VALID position (the `(0,0,P1)` origin present, no
     * duplicate/contradictory coords — `GameState::from_state` seeds the origin
     * and panics otherwise). For an invalid position returns `Err(JsError)`
     * with a clear message; defense analysis is inherently about a game
     * position's turn, so requiring game-validity is consistent with the deep
     * provers (Task 4).
     * @param {Position} position
     * @param {SolverLimits} limits
     * @returns {DefenseOutcome}
     */
    solve_defense(position, limits) {
        _assertClass(position, Position);
        _assertClass(limits, SolverLimits);
        const ret = wasm.strixsolver_solve_defense(this.__wbg_ptr, position.__wbg_ptr, limits.__wbg_ptr);
        if (ret[2]) {
            throw takeFromExternrefTable0(ret[1]);
        }
        return DefenseOutcome.__wrap(ret[0]);
    }
    /**
     * `solve_defense` with the wide (threat + quiet-builder) generator for the
     * threat sighting AND every candidate/pair verification — sees threats the
     * tight defense provably cannot (a forcing turn pairing a threat stone
     * with a quiet build stone), at the known ~1.5-1.7x per-solve cost.
     * @param {Position} position
     * @param {SolverLimits} limits
     * @returns {DefenseOutcome}
     */
    solve_defense_wide(position, limits) {
        _assertClass(position, Position);
        _assertClass(limits, SolverLimits);
        const ret = wasm.strixsolver_solve_defense_wide(this.__wbg_ptr, position.__wbg_ptr, limits.__wbg_ptr);
        if (ret[2]) {
            throw takeFromExternrefTable0(ret[1]);
        }
        return DefenseOutcome.__wrap(ret[0]);
    }
    /**
     * Opponent's forcing win if left unblocked: flip `to_move` and solve.
     * Does NOT call `forcing::solve_threat` (GameState-based); the flip +
     * re-solve IS the threat semantics.
     * @param {Position} position
     * @param {SolverLimits} limits
     * @returns {SolveOutcome}
     */
    solve_threat(position, limits) {
        _assertClass(position, Position);
        _assertClass(limits, SolverLimits);
        const ret = wasm.strixsolver_solve_threat(this.__wbg_ptr, position.__wbg_ptr, limits.__wbg_ptr);
        if (ret[2]) {
            throw takeFromExternrefTable0(ret[1]);
        }
        return SolveOutcome.__wrap(ret[0]);
    }
    /**
     * Wide-generator counterpart to `solve_threat`, used by Observatory's
     * automatic per-position analysis.
     * @param {Position} position
     * @param {SolverLimits} limits
     * @returns {SolveOutcome}
     */
    solve_threat_wide(position, limits) {
        _assertClass(position, Position);
        _assertClass(limits, SolverLimits);
        const ret = wasm.strixsolver_solve_threat_wide(this.__wbg_ptr, position.__wbg_ptr, limits.__wbg_ptr);
        if (ret[2]) {
            throw takeFromExternrefTable0(ret[1]);
        }
        return SolveOutcome.__wrap(ret[0]);
    }
    /**
     * @param {Position} position
     * @param {SolverLimits} limits
     * @returns {SolveOutcome}
     */
    solve_wide(position, limits) {
        _assertClass(position, Position);
        _assertClass(limits, SolverLimits);
        const ret = wasm.strixsolver_solve_wide(this.__wbg_ptr, position.__wbg_ptr, limits.__wbg_ptr);
        if (ret[2]) {
            throw takeFromExternrefTable0(ret[1]);
        }
        return SolveOutcome.__wrap(ret[0]);
    }
    /**
     * Replay and verify a serialized PDS-PN all-defense certificate from
     * scratch. This does not consult a search tree, proof numbers, or TT: every
     * attacker action and the complete set of exact defender replies is
     * recomputed by the shared forcing kernel.
     * @param {Position} position
     * @param {string} certificate_json
     * @returns {CertificateVerification}
     */
    verify_certificate(position, certificate_json) {
        _assertClass(position, Position);
        const ptr0 = passStringToWasm0(certificate_json, wasm.__wbindgen_malloc, wasm.__wbindgen_realloc);
        const len0 = WASM_VECTOR_LEN;
        const ret = wasm.strixsolver_verify_certificate(this.__wbg_ptr, position.__wbg_ptr, ptr0, len0);
        if (ret[2]) {
            throw takeFromExternrefTable0(ret[1]);
        }
        return CertificateVerification.__wrap(ret[0]);
    }
}
if (Symbol.dispose) StrixSolver.prototype[Symbol.dispose] = StrixSolver.prototype.free;

export class Turn {
    static __wrap(ptr) {
        ptr = ptr >>> 0;
        const obj = Object.create(Turn.prototype);
        obj.__wbg_ptr = ptr;
        TurnFinalization.register(obj, obj.__wbg_ptr, obj);
        return obj;
    }
    static __unwrap(jsValue) {
        if (!(jsValue instanceof Turn)) {
            return 0;
        }
        return jsValue.__destroy_into_raw();
    }
    __destroy_into_raw() {
        const ptr = this.__wbg_ptr;
        this.__wbg_ptr = 0;
        TurnFinalization.unregister(this);
        return ptr;
    }
    free() {
        const ptr = this.__destroy_into_raw();
        wasm.__wbg_turn_free(ptr, 0);
    }
    /**
     * @returns {CoordW[]}
     */
    get cells() {
        const ret = wasm.__wbg_get_turn_cells(this.__wbg_ptr);
        var v1 = getArrayJsValueFromWasm0(ret[0], ret[1]).slice();
        wasm.__wbindgen_free(ret[0], ret[1] * 4, 4);
        return v1;
    }
    /**
     * @returns {Player}
     */
    get player() {
        const ret = wasm.__wbg_get_turn_player(this.__wbg_ptr);
        return ret;
    }
    /**
     * @returns {number}
     */
    get turn() {
        const ret = wasm.__wbg_get_turn_turn(this.__wbg_ptr);
        return ret >>> 0;
    }
    /**
     * @param {CoordW[]} arg0
     */
    set cells(arg0) {
        const ptr0 = passArrayJsValueToWasm0(arg0, wasm.__wbindgen_malloc);
        const len0 = WASM_VECTOR_LEN;
        wasm.__wbg_set_turn_cells(this.__wbg_ptr, ptr0, len0);
    }
    /**
     * @param {Player} arg0
     */
    set player(arg0) {
        wasm.__wbg_set_turn_player(this.__wbg_ptr, arg0);
    }
    /**
     * @param {number} arg0
     */
    set turn(arg0) {
        wasm.__wbg_set_turn_turn(this.__wbg_ptr, arg0);
    }
}
if (Symbol.dispose) Turn.prototype[Symbol.dispose] = Turn.prototype.free;

function __wbg_get_imports() {
    const import0 = {
        __proto__: null,
        __wbg_Error_55538483de6e3abe: function(arg0, arg1) {
            const ret = Error(getStringFromWasm0(arg0, arg1));
            return ret;
        },
        __wbg___wbindgen_throw_5549492daedad139: function(arg0, arg1) {
            throw new Error(getStringFromWasm0(arg0, arg1));
        },
        __wbg_coordw_new: function(arg0) {
            const ret = CoordW.__wrap(arg0);
            return ret;
        },
        __wbg_coordw_unwrap: function(arg0) {
            const ret = CoordW.__unwrap(arg0);
            return ret;
        },
        __wbg_minimumdefensecover_new: function(arg0) {
            const ret = MinimumDefenseCover.__wrap(arg0);
            return ret;
        },
        __wbg_minimumdefensecover_unwrap: function(arg0) {
            const ret = MinimumDefenseCover.__unwrap(arg0);
            return ret;
        },
        __wbg_pairanchor_new: function(arg0) {
            const ret = PairAnchor.__wrap(arg0);
            return ret;
        },
        __wbg_pairanchor_unwrap: function(arg0) {
            const ret = PairAnchor.__unwrap(arg0);
            return ret;
        },
        __wbg_stone_new: function(arg0) {
            const ret = Stone.__wrap(arg0);
            return ret;
        },
        __wbg_stone_unwrap: function(arg0) {
            const ret = Stone.__unwrap(arg0);
            return ret;
        },
        __wbg_turn_new: function(arg0) {
            const ret = Turn.__wrap(arg0);
            return ret;
        },
        __wbg_turn_unwrap: function(arg0) {
            const ret = Turn.__unwrap(arg0);
            return ret;
        },
        __wbindgen_init_externref_table: function() {
            const table = wasm.__wbindgen_externrefs;
            const offset = table.grow(4);
            table.set(0, undefined);
            table.set(offset + 0, undefined);
            table.set(offset + 1, null);
            table.set(offset + 2, true);
            table.set(offset + 3, false);
        },
    };
    return {
        __proto__: null,
        "./hexo_solver_wasm_bg.js": import0,
    };
}

const CertificateVerificationFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_certificateverification_free(ptr >>> 0, 1));
const CoordWFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_coordw_free(ptr >>> 0, 1));
const DefenseOutcomeFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_defenseoutcome_free(ptr >>> 0, 1));
const MinimumDefenseCoverFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_minimumdefensecover_free(ptr >>> 0, 1));
const MinimumDefenseOutcomeFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_minimumdefenseoutcome_free(ptr >>> 0, 1));
const PairAnchorFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_pairanchor_free(ptr >>> 0, 1));
const PositionFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_position_free(ptr >>> 0, 1));
const ShortestOutcomeFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_shortestoutcome_free(ptr >>> 0, 1));
const SolveOutcomeFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_solveoutcome_free(ptr >>> 0, 1));
const SolverLimitsFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_solverlimits_free(ptr >>> 0, 1));
const StoneFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_stone_free(ptr >>> 0, 1));
const StrixSolverFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_strixsolver_free(ptr >>> 0, 1));
const TurnFinalization = (typeof FinalizationRegistry === 'undefined')
    ? { register: () => {}, unregister: () => {} }
    : new FinalizationRegistry(ptr => wasm.__wbg_turn_free(ptr >>> 0, 1));

function addToExternrefTable0(obj) {
    const idx = wasm.__externref_table_alloc();
    wasm.__wbindgen_externrefs.set(idx, obj);
    return idx;
}

function _assertClass(instance, klass) {
    if (!(instance instanceof klass)) {
        throw new Error(`expected instance of ${klass.name}`);
    }
}

function getArrayJsValueFromWasm0(ptr, len) {
    ptr = ptr >>> 0;
    const mem = getDataViewMemory0();
    const result = [];
    for (let i = ptr; i < ptr + 4 * len; i += 4) {
        result.push(wasm.__wbindgen_externrefs.get(mem.getUint32(i, true)));
    }
    wasm.__externref_drop_slice(ptr, len);
    return result;
}

let cachedDataViewMemory0 = null;
function getDataViewMemory0() {
    if (cachedDataViewMemory0 === null || cachedDataViewMemory0.buffer.detached === true || (cachedDataViewMemory0.buffer.detached === undefined && cachedDataViewMemory0.buffer !== wasm.memory.buffer)) {
        cachedDataViewMemory0 = new DataView(wasm.memory.buffer);
    }
    return cachedDataViewMemory0;
}

function getStringFromWasm0(ptr, len) {
    ptr = ptr >>> 0;
    return decodeText(ptr, len);
}

let cachedUint32ArrayMemory0 = null;
function getUint32ArrayMemory0() {
    if (cachedUint32ArrayMemory0 === null || cachedUint32ArrayMemory0.byteLength === 0) {
        cachedUint32ArrayMemory0 = new Uint32Array(wasm.memory.buffer);
    }
    return cachedUint32ArrayMemory0;
}

let cachedUint8ArrayMemory0 = null;
function getUint8ArrayMemory0() {
    if (cachedUint8ArrayMemory0 === null || cachedUint8ArrayMemory0.byteLength === 0) {
        cachedUint8ArrayMemory0 = new Uint8Array(wasm.memory.buffer);
    }
    return cachedUint8ArrayMemory0;
}

function isLikeNone(x) {
    return x === undefined || x === null;
}

function passArray32ToWasm0(arg, malloc) {
    const ptr = malloc(arg.length * 4, 4) >>> 0;
    getUint32ArrayMemory0().set(arg, ptr / 4);
    WASM_VECTOR_LEN = arg.length;
    return ptr;
}

function passArrayJsValueToWasm0(array, malloc) {
    const ptr = malloc(array.length * 4, 4) >>> 0;
    for (let i = 0; i < array.length; i++) {
        const add = addToExternrefTable0(array[i]);
        getDataViewMemory0().setUint32(ptr + 4 * i, add, true);
    }
    WASM_VECTOR_LEN = array.length;
    return ptr;
}

function passStringToWasm0(arg, malloc, realloc) {
    if (realloc === undefined) {
        const buf = cachedTextEncoder.encode(arg);
        const ptr = malloc(buf.length, 1) >>> 0;
        getUint8ArrayMemory0().subarray(ptr, ptr + buf.length).set(buf);
        WASM_VECTOR_LEN = buf.length;
        return ptr;
    }

    let len = arg.length;
    let ptr = malloc(len, 1) >>> 0;

    const mem = getUint8ArrayMemory0();

    let offset = 0;

    for (; offset < len; offset++) {
        const code = arg.charCodeAt(offset);
        if (code > 0x7F) break;
        mem[ptr + offset] = code;
    }
    if (offset !== len) {
        if (offset !== 0) {
            arg = arg.slice(offset);
        }
        ptr = realloc(ptr, len, len = offset + arg.length * 3, 1) >>> 0;
        const view = getUint8ArrayMemory0().subarray(ptr + offset, ptr + len);
        const ret = cachedTextEncoder.encodeInto(arg, view);

        offset += ret.written;
        ptr = realloc(ptr, len, offset, 1) >>> 0;
    }

    WASM_VECTOR_LEN = offset;
    return ptr;
}

function takeFromExternrefTable0(idx) {
    const value = wasm.__wbindgen_externrefs.get(idx);
    wasm.__externref_table_dealloc(idx);
    return value;
}

let cachedTextDecoder = new TextDecoder('utf-8', { ignoreBOM: true, fatal: true });
cachedTextDecoder.decode();
const MAX_SAFARI_DECODE_BYTES = 2146435072;
let numBytesDecoded = 0;
function decodeText(ptr, len) {
    numBytesDecoded += len;
    if (numBytesDecoded >= MAX_SAFARI_DECODE_BYTES) {
        cachedTextDecoder = new TextDecoder('utf-8', { ignoreBOM: true, fatal: true });
        cachedTextDecoder.decode();
        numBytesDecoded = len;
    }
    return cachedTextDecoder.decode(getUint8ArrayMemory0().subarray(ptr, ptr + len));
}

const cachedTextEncoder = new TextEncoder();

if (!('encodeInto' in cachedTextEncoder)) {
    cachedTextEncoder.encodeInto = function (arg, view) {
        const buf = cachedTextEncoder.encode(arg);
        view.set(buf);
        return {
            read: arg.length,
            written: buf.length
        };
    };
}

let WASM_VECTOR_LEN = 0;

let wasmModule, wasm;
function __wbg_finalize_init(instance, module) {
    wasm = instance.exports;
    wasmModule = module;
    cachedDataViewMemory0 = null;
    cachedUint32ArrayMemory0 = null;
    cachedUint8ArrayMemory0 = null;
    wasm.__wbindgen_start();
    return wasm;
}

async function __wbg_load(module, imports) {
    if (typeof Response === 'function' && module instanceof Response) {
        if (typeof WebAssembly.instantiateStreaming === 'function') {
            try {
                return await WebAssembly.instantiateStreaming(module, imports);
            } catch (e) {
                const validResponse = module.ok && expectedResponseType(module.type);

                if (validResponse && module.headers.get('Content-Type') !== 'application/wasm') {
                    console.warn("`WebAssembly.instantiateStreaming` failed because your server does not serve Wasm with `application/wasm` MIME type. Falling back to `WebAssembly.instantiate` which is slower. Original error:\n", e);

                } else { throw e; }
            }
        }

        const bytes = await module.arrayBuffer();
        return await WebAssembly.instantiate(bytes, imports);
    } else {
        const instance = await WebAssembly.instantiate(module, imports);

        if (instance instanceof WebAssembly.Instance) {
            return { instance, module };
        } else {
            return instance;
        }
    }

    function expectedResponseType(type) {
        switch (type) {
            case 'basic': case 'cors': case 'default': return true;
        }
        return false;
    }
}

function initSync(module) {
    if (wasm !== undefined) return wasm;


    if (module !== undefined) {
        if (Object.getPrototypeOf(module) === Object.prototype) {
            ({module} = module)
        } else {
            console.warn('using deprecated parameters for `initSync()`; pass a single object instead')
        }
    }

    const imports = __wbg_get_imports();
    if (!(module instanceof WebAssembly.Module)) {
        module = new WebAssembly.Module(module);
    }
    const instance = new WebAssembly.Instance(module, imports);
    return __wbg_finalize_init(instance, module);
}

async function __wbg_init(module_or_path) {
    if (wasm !== undefined) return wasm;


    if (module_or_path !== undefined) {
        if (Object.getPrototypeOf(module_or_path) === Object.prototype) {
            ({module_or_path} = module_or_path)
        } else {
            console.warn('using deprecated parameters for the initialization function; pass a single object instead')
        }
    }

    if (module_or_path === undefined) {
        module_or_path = new URL('hexo_solver_wasm_bg.wasm', import.meta.url);
    }
    const imports = __wbg_get_imports();

    if (typeof module_or_path === 'string' || (typeof Request === 'function' && module_or_path instanceof Request) || (typeof URL === 'function' && module_or_path instanceof URL)) {
        module_or_path = fetch(module_or_path);
    }

    const { instance, module } = await __wbg_load(await module_or_path, imports);

    return __wbg_finalize_init(instance, module);
}

export { initSync, __wbg_init as default };
