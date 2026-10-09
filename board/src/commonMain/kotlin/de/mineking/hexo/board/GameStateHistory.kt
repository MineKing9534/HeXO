package de.mineking.hexo.board

interface GameStateHistory {
    val numberOfStates: Int
    fun getState(index: Int): Board
}

val GameStateHistory.finalState get() = getState(numberOfStates - 1)

open class PartialGameStateHistory(val state: Board, val turns: GamePosition<*>) : GameStateHistory {
    fun hasState() = !state.isEmpty(includeHighlights = false)

    override val numberOfStates = if (hasState()) turns.numberOfStates + 1 else turns.numberOfStates
    override fun getState(index: Int): Board {
        if (!hasState()) return state + turns.getState(index)
        return state + turns.getState(index - 1)
    }

    operator fun component1() = state
    operator fun component2() = turns
}

fun PartialGameStateHistory.toGamePosition() = state.toGamePositionForce() + turns
