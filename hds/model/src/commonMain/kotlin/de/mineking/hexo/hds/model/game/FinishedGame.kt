package de.mineking.hexo.hds.model.game

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.hds.model.Instant

interface FinishedGame : Game {
    val url: String

    override val result: GameResult
    override val moves: List<FinishedGameMove>
    override val players: List<FinishedGamePlayer>
}

interface FinishedGamePlayer : Player {
    val eloChange: Int?
}

class FinishedGameMove(
    coordinate: CellCoordinate,
    player: Player,
    val timestamp: Instant,
) : GameMove(coordinate, player)
