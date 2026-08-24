package de.mineking.hexo.game.model.game

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.GamePosition
import kotlin.time.Instant

interface FinishedGame : Game {
    val url: String

    override val result: GameResult
    override val players: List<FinishedGamePlayer>
}

interface FinishedGameWithPosition : FinishedGame, GameWithPosition {
    override val position: GamePosition<FinishedGameMove>
}

interface FinishedGamePlayer : Player {
    val eloChange: Int?
}

class FinishedGameMove(
    coordinate: CellCoordinate,
    player: Player,
    val timestamp: Instant,
) : GameMove(coordinate, player)
