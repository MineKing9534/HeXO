package de.mineking.hexo.watchparty.common

import de.mineking.hexo.board.Board
import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellOverride
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.hds.model.game.GameId
import de.mineking.hexo.hds.model.session.SessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WatchPartyRequest

@Serializable
@SerialName("ping")
data object WatchPartyPingRequest : WatchPartyRequest

@Serializable
sealed interface WatchPartyNavigateTarget {
    @Serializable
    @SerialName("sandbox")
    data object Sandbox : WatchPartyNavigateTarget

    @Serializable
    @SerialName("session")
    data class Session(val id: SessionId) : WatchPartyNavigateTarget

    @Serializable
    @SerialName("game")
    data class Game(val id: GameId) : WatchPartyNavigateTarget
}

@Serializable
@SerialName("navigate")
data class WatchPartyNavigateRequest(
    val target: WatchPartyNavigateTarget?,
) : WatchPartyRequest

@Serializable
@SerialName("move")
data class WatchPartyMoveCountRequest(
    val move: Int,
) : WatchPartyRequest

@Serializable
@SerialName("update")
data class WatchPartyUpdateRequest(
    val board: Board,
) : WatchPartyRequest

@Serializable
@SerialName("cell")
data class WatchPartyCellRequest(
    val coordinate: CellCoordinate,
    val cell: CellOverride,
) : WatchPartyRequest

@Serializable
@SerialName("line")
data class WatchPartyLineHighlightRequest(
    val line: LineHighlight,
    val remove: Boolean,
) : WatchPartyRequest

@Serializable
@SerialName("clear-highlights")
data object WatchPartyClearHighlightsRequest : WatchPartyRequest
