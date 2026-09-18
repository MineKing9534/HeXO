package de.mineking.hexo.watchparty.client

import de.mineking.hexo.board.Board
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.utils.types.Entity
import de.mineking.hexo.watchparty.model.WatchPartyId
import de.mineking.hexo.watchparty.protocol.WatchPartyDto
import de.mineking.hexo.watchparty.protocol.WatchPartyTargetDto

class WatchPartyView internal constructor(
    client: WatchPartyClient,
    data: WatchPartyDto,
) : Entity<WatchPartyId> {
    override val id = data.id
    override val url = "${client.publicUrl}/watchparty/${id.value}"

    val revision = data.revision
    val generation = data.generation
    val hasClearableHighlights = data.clearableHighlights
    val target = data.target?.toView(data)
}

sealed interface WatchPartyTargetView {
    val generation: Long

    data class Sandbox(
        override val generation: Long,
        val board: Board,
    ) : WatchPartyTargetView

    data class FinishedGame(
        override val generation: Long,
        val targetId: GameId,
        val overlay: Board,
        val currentMove: Int,
    ) : WatchPartyTargetView

    data class Session(
        override val generation: Long,
        val targetId: SessionId,
        val overlay: Board,
        val currentMove: Int,
    ) : WatchPartyTargetView
}

private fun WatchPartyTargetDto.toView(data: WatchPartyDto) = when (this) {
    is WatchPartyTargetDto.Sandbox -> WatchPartyTargetView.Sandbox(
        generation = data.generation,
        board = board,
    )
    is WatchPartyTargetDto.Game -> WatchPartyTargetView.FinishedGame(
        generation = data.generation,
        targetId = gameId,
        overlay = overlay,
        currentMove = move,
    )
    is WatchPartyTargetDto.Session -> WatchPartyTargetView.Session(
        generation = data.generation,
        targetId = sessionId,
        overlay = overlay,
        currentMove = move,
    )
}
